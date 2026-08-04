package com.starfall.scene;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector3;
import com.starfall.StarfallGame;
import com.starfall.game.ActionQueue;
import com.starfall.game.ActionResult;
import com.starfall.game.Arena;
import com.starfall.game.ArenaLayout;
import com.starfall.game.ArenaSetup;
import com.starfall.game.Direction;
import com.starfall.game.Enemy;
import com.starfall.game.Hero;
import com.starfall.game.HudLayout;
import com.starfall.game.Intention;
import com.starfall.game.Occupant;
import com.starfall.game.Tile;
import com.starfall.game.TilePreview;
import com.starfall.game.Trait;
import com.starfall.render.PixelFont;
import com.starfall.render.PixelPainter;

import java.util.ArrayList;
import java.util.List;

/**
 * Le jeu : une grille linéaire, un héros, ses tuiles, et les ennemis qui annoncent leurs coups.
 *
 * <h2>L'interface, et pourquoi elle a quitté le bandeau de diagnostic</h2>
 *
 * <p>Jusqu'ici l'état du jeu s'écrivait dans un bandeau noir posé dans le coin de la <b>fenêtre</b>,
 * en huit lignes de capitales. Ça marchait comme instrument de mise au point et ça ne marchait pas
 * du tout comme interface, pour trois raisons qui se cumulaient :
 *
 * <ul>
 *   <li>il vivait en pixels-écran, donc il n'était pas au même endroit que ce dont il parlait, et
 *       il rétrécissait ou grandissait indépendamment du plateau ;</li>
 *   <li>il disait des choses que la scène montrait déjà — la vie, l'orientation, les intentions —
 *       et le joueur devait choisir laquelle des deux sources croire ;</li>
 *   <li>et surtout il <b>ne disait pas</b> la seule chose que le plateau ne montrait pas : la
 *       portée des tuiles. L'estoc porte à deux cases, la frappe à une, et rien à l'écran ne
 *       l'indiquait. Le joueur devait l'apprendre en gâchant des tuiles.</li>
 * </ul>
 *
 * <p>Tout ce qui concerne le jeu vit donc maintenant en pixels-monde, sur la même grille de pixels
 * que le plateau : bandeau d'état en haut, panneau d'information juste au-dessus des têtes, repères
 * de portée sous les dalles, infobulles au survol, aide en {@code F1}. Le bandeau de diagnostic
 * reste disponible pour la mire de calibration, qui est un instrument et non un jeu.
 */
public final class ArenaScene implements Scene {

    private static final Color SKY = new Color(0x141a2eff);
    private static final Color WALL = new Color(0x1c2440ff);
    private static final Color PIT = new Color(0x0c101eff);
    private static final Color CELL_LINE = new Color(0x39456bff);
    private static final Color HERO_MARK = new Color(0x54d6ffff);
    private static final Color TARGET_MARK = new Color(0xffcc33ff);
    private static final Color HOVER_MARK = HudColors.HOVER;

    private SceneContext context;
    private PixelPainter painter;
    private PixelFont font;
    private Arena arena;
    private ArenaLayout layout;
    private HudLayout hud;

    private int hoveredCell = -1;
    private int hoveredQueueSlot = -1;
    private int hoveredRackSlot = -1;
    private ActionResult lastResult;

    /**
     * L'aide est <b>ouverte au départ</b> et se referme au premier geste.
     *
     * <p>Un jeu au tour par tour dont les commandes ne sont écrites nulle part n'est pas jouable ;
     * un jeu qui affiche ses commandes en permanence gaspille la moitié de son écran. Le compromis
     * est de les montrer tant que le joueur n'a rien fait, et de lui rendre la place dès qu'il a
     * commencé — {@code F1} les rappelle à tout moment.
     */
    private boolean helpVisible = true;

    @Override
    public String name() {
        return "arena";
    }

    @Override
    public void create(SceneContext context) {
        this.context = context;
        this.painter = context.painter();
        this.font = context.font();

        int gridWidth = context.options().gridWidth;
        arena = ArenaSetup.trainingArena(gridWidth);
        layout = new ArenaLayout(gridWidth, StarfallGame.MIN_WORLD_WIDTH / 2);
        hud = new HudLayout(StarfallGame.MIN_WORLD_WIDTH / 2, arena.rack().tiles().size());
    }

    /**
     * Scénario joué en mode capture, une action par image.
     *
     * <p>Les entrées sont coupées en capture, donc sans cela toutes les images d'une série seraient
     * identiques. Rejouer un scénario court les rend différentes <em>et</em> utiles : la planche de
     * contact montre le jeu à l'œuvre au lieu de trois copies de la position de départ.
     */
    @Override
    public void act(float time, int frameIndex, boolean interactive) {
        if (!interactive) {
            replayScript(frameIndex);
            return;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.F1)) {
            helpVisible = !helpVisible;
            return;
        }
        // Une seule action par image, quelle qu'en soit la source. La souris respectait déjà la
        // règle, le clavier non : trois « if » indépendants laissaient D et E dans la même image
        // produire un déplacement PUIS un échange, soit deux tours consommés d'un coup.
        if (!readMouse()) {
            readKeyboard();
        }
    }

    /**
     * Enregistre le résultat d'une action.
     *
     * <p>Point de passage unique, pour la même raison que {@code consumeTurn} côté modèle : c'est
     * ici, et nulle part ailleurs, que l'aide se referme. Éparpillé sur les huit endroits qui
     * déclenchent une action, ce geste aurait été oublié par le neuvième.
     */
    private boolean applied(ActionResult result) {
        lastResult = result;
        helpVisible = false;
        return true;
    }

    /**
     * Rejoue les {@code frameIndex} premières actions du scénario depuis un état neuf.
     *
     * <p>Rejouer depuis le début plutôt qu'appliquer une action de plus est ce qui garde la capture
     * reproductible : {@code act} est appelée à chaque image rendue, pas une fois par image écrite.
     *
     * <p>Le survol est scénarisé lui aussi. Sans cela l'infobulle et le repère de portée — c'est-à-
     * dire l'essentiel de ce que ce jalon ajoute — n'apparaîtraient sur <em>aucune</em> planche de
     * contact, et une planche qui ne montre pas ce qu'elle légende vaut moins que pas de planche.
     */
    private void replayScript(int frameIndex) {
        hoveredCell = -1;
        hoveredQueueSlot = -1;
        hoveredRackSlot = frameIndex < HOVER_SCRIPT.length ? HOVER_SCRIPT[frameIndex] : -1;
        helpVisible = frameIndex == 0;

        if (frameIndex == scriptedFrame) {
            return;
        }
        arena = ArenaSetup.trainingArena(layout.gridWidth());

        for (int i = 0; i < frameIndex && i < SCRIPT.length; i++) {
            lastResult = SCRIPT[i].applyTo(arena);
        }
        if (frameIndex > SCRIPT.length && !exhaustionReported) {
            // Le dire plutôt que de laisser un relecteur croire que deux images identiques
            // signalent un rendu figé.
            System.out.println("[Starfall] le scénario de capture compte " + SCRIPT.length
                    + " actions ; les images suivantes répètent l'état final");
            exhaustionReported = true;
        }
        scriptedFrame = frameIndex;
    }

    /** Une action du scénario de capture. */
    private interface ScriptedAction {
        ActionResult applyTo(Arena arena);
    }

    /**
     * Le scénario montre la file d'actions et les portées : on charge, on regarde la file se
     * remplir, puis on la dépile — et l'on voit qu'elle se vide à l'envers de l'ordre où on l'a
     * remplie, la ligne d'annonce disant à chaque étape ce qui partira.
     */
    private static final ScriptedAction[] SCRIPT = {
            a -> a.queueTile(Tile.THRUST),   // l'estoc porte à deux cases : c'est la portée utile
            a -> a.queueTile(Tile.STRIKE),   // charger la file ne coûte rien : le tour reste à zéro
            a -> a.unqueueAt(1),             // on se ravise, gratuitement aussi
            a -> a.executeTop(),             // l'estoc tombe sur l'ennemi de droite
            a -> a.step(Direction.LEFT),     // demi-tour vers l'archer
            a -> a.step(Direction.LEFT),     // on avance : les ennemis jouent, la vie descend
            a -> a.queueTile(Tile.STRIKE),
            a -> a.executeTop(),             // et la vague bascule quand le terrain se vide
            a -> a.step(Direction.RIGHT),    // on va au contact du premier de la vague suivante
            a -> a.queueTile(Tile.PUSH),     // la poussée annonce sa trajectoire, case par case
            a -> a.queueTile(Tile.STRIKE),
    };

    /**
     * Tuile du râtelier survolée à chaque image de la capture, ou {@code -1}.
     *
     * <p>L'ordre du râtelier est celui de {@code ArenaSetup} : frappe, estoc, poussée, élan, pas de
     * côté, volte-face.
     */
    private static final int[] HOVER_SCRIPT = {
            -1,  // image 0 : l'aide, telle qu'un joueur la découvre
            1,   // l'estoc et sa portée 2
            -1,  // rien de survolé : le préavis résolu du sommet, et il annonce un coup dans le vide
            2,   // la poussée
            3,   // l'élan, dont la portée dépend du terrain
            -1,
            4,   // le pas de côté, tuile Free-Play
            -1,
            -1,
            -1,  // le préavis résolu d'une poussée : sa trajectoire et son arrivée
            5,   // la volte-face, qui ne vise aucune case
    };

    private int scriptedFrame = -1;
    private boolean exhaustionReported;

    /**
     * @return vrai si une action a été déclenchée ; la souris et le clavier se partagent la règle
     *         « une action par image »
     */
    private boolean readMouse() {
        Vector3 world = context.viewport().unproject(
                new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0f));
        hoveredCell = layout.cellAt(world.x, world.y);
        hoveredQueueSlot = hud.queueSlotAt(world.x, world.y);
        hoveredRackSlot = hud.rackSlotAt(world.x, world.y);

        if (!Gdx.input.justTouched()) {
            return false;
        }
        // Les trois zones sont disjointes par construction ; l'ordre ne fait que rendre
        // l'intention explicite.
        if (hoveredRackSlot >= 0) {
            return applied(arena.queueTile(arena.rack().tiles().get(hoveredRackSlot)));
        }
        if (hoveredQueueSlot >= 0) {
            return applied(arena.unqueueAt(hoveredQueueSlot));
        }
        if (hoveredCell >= 0) {
            return applied(arena.clickOn(hoveredCell));
        }
        return false;
    }

    /**
     * Clavier.
     *
     * <p><b>Q est lié en plus de A, et ce n'est pas une commodité.</b> libGDX rapporte les touches
     * par <em>position physique</em> sur une disposition américaine : {@code Keys.A} désigne la
     * touche à l'emplacement du A américain, qui porte la lettre <b>Q</b> sur un clavier AZERTY. Sur
     * la machine de développement, appuyer sur la touche marquée A ne faisait donc rien du tout —
     * vérifié en pilotant la vraie fenêtre. Pour un jeu en français, lier les deux positions est le
     * minimum ; les flèches restent de toute façon la commande principale.
     */
    private void readKeyboard() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.LEFT)
                || Gdx.input.isKeyJustPressed(Input.Keys.A)
                || Gdx.input.isKeyJustPressed(Input.Keys.Q)) {
            applied(arena.step(Direction.LEFT));
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.RIGHT)
                || Gdx.input.isKeyJustPressed(Input.Keys.D)) {
            applied(arena.step(Direction.RIGHT));
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.E)) {
            applied(arena.swapWithTarget());
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)
                || Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
            applied(arena.executeTop());
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.BACKSPACE)
                || Gdx.input.isKeyJustPressed(Input.Keys.FORWARD_DEL)) {
            applied(arena.unqueueAt(arena.queue().size() - 1));
        } else {
            readTileKeys();
        }
    }

    /**
     * Chiffres 1 à 6 : poser la tuile correspondante du râtelier.
     *
     * <p>Les touches suivent l'ordre d'affichage du râtelier, pas un ordre interne : le joueur
     * compte ce qu'il voit.
     */
    private void readTileKeys() {
        List<Tile> tiles = arena.rack().tiles();
        for (int i = 0; i < tiles.size() && i < 9; i++) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1 + i)
                    || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_1 + i)) {
                applied(arena.queueTile(tiles.get(i)));
                return;
            }
        }
    }

    @Override
    public void drawWorld() {
        drawBackdrop();
        drawGround();
        drawThreats();
        drawReach();
        drawOccupants();
        drawIntentions();
        drawTacticalMarks();
        // Le râtelier d'abord : ses repères ne doivent jamais pouvoir recouvrir ceux de la file,
        // même si les deux bandes venaient à se croiser un jour.
        drawRack();
        drawQueue();
        // Puis l'interface, par-dessus tout le reste : un panneau doit couvrir la scène, jamais
        // l'inverse.
        drawBanner();
        drawInfoPanel();
        drawOutcome();
        drawHelp();
        painter.color(Color.WHITE);
    }

    // ------------------------------------------------------------------ portées

    /**
     * Les repères de portée, dans leur bande sous les dalles.
     *
     * <p>Deux régimes, et la différence entre les deux est le point délicat du jalon.
     *
     * <ul>
     *   <li><b>Le sommet de la file</b> est <em>résolu</em> : il s'exécutera contre l'état actuel du
     *       plateau, donc on a le droit d'annoncer ce qu'il fera, case par case et résultat compris.
     *       Repères <b>pleins</b>.</li>
     *   <li><b>Une tuile survolée</b> ne s'exécutera pas tout de suite : elle passera d'abord par la
     *       file, derrière les tuiles déjà posées, et le plateau aura changé d'ici là. Lui prêter un
     *       résultat serait exactement le mensonge que le télégraphe a coûté deux jalons à
     *       éteindre. On ne montre donc que sa <em>portée</em>, c'est-à-dire les cases qu'elle peut
     *       atteindre depuis la position actuelle. Repères <b>creux</b>.</li>
     * </ul>
     *
     * <p>Plein contre creux : la forme dit « ceci arrivera » ou « ceci pourrait atteindre », sans
     * qu'il faille lire un libellé.
     */
    private void drawReach() {
        Tile hovered = hoveredTile();
        TilePreview preview = hovered != null ? arena.preview(hovered) : arena.previewTop();
        if (preview != null) {
            drawPreview(preview, hovered == null);
        }
    }

    /** Tuile survolée, du râtelier ou de la file, ou {@code null}. */
    private Tile hoveredTile() {
        if (hoveredRackSlot >= 0 && hoveredRackSlot < arena.rack().tiles().size()) {
            return arena.rack().tiles().get(hoveredRackSlot);
        }
        List<Tile> queued = arena.queue().fromOldest();
        if (hoveredQueueSlot >= 0 && hoveredQueueSlot < queued.size()) {
            return queued.get(hoveredQueueSlot);
        }
        return null;
    }

    /**
     * Dessine un préavis. {@code committed} distingue les deux régimes : plein pour ce qui va se
     * produire, creux pour ce que la tuile survolée pourrait atteindre.
     */
    private void drawPreview(TilePreview preview, boolean committed) {
        switch (preview.kind()) {
            case HIT -> {
                drawReachLink(arena.heroCell(), preview.aim());
                drawReachBar(preview.aim(), HudColors.PREVIEW, committed);
            }
            case PUSH -> {
                drawReachBar(preview.aim(), HudColors.PREVIEW, committed);
                drawPushTrack(preview, committed);
            }
            case MOVE -> {
                drawReachLink(arena.heroCell(), preview.landing());
                drawReachBar(preview.landing(), HudColors.PREVIEW, committed);
            }
            // Le demi-tour ne vise aucune case : un repère sous le héros, et la ligne d'annonce dit
            // vers où il regardera.
            case TURN -> drawReachBar(arena.heroCell(), HudColors.PREVIEW, committed);
            // Une tuile qui ne portera pas mérite d'être annoncée : elle sera dépensée quand même.
            // Éteinte plutôt qu'absente, sinon rien ne distingue « ça rate » de « rien en file ».
            case NONE -> {
                if (arena.grid().contains(preview.aim())) {
                    drawReachBar(preview.aim(), HudColors.DIMMED, committed);
                }
            }
        }
    }

    private void drawReachBar(int cell, Color color, boolean committed) {
        int x = layout.cellLeft(cell) + 2;
        int width = ArenaLayout.CELL_WIDTH - 4;
        if (committed) {
            painter.fill(x, ArenaLayout.PREVIEW_Y, width, ArenaLayout.PREVIEW_HEIGHT, color);
        } else {
            painter.outline(x, ArenaLayout.PREVIEW_Y, width, ArenaLayout.PREVIEW_HEIGHT, color);
        }
    }

    /** Trait entre le héros et la case concernée, dans la bande des portées. */
    private void drawReachLink(int from, int to) {
        int a = cellCentre(from);
        int b = cellCentre(to);
        painter.fill(Math.min(a, b), ArenaLayout.PREVIEW_Y + 1, Math.abs(b - a), 1,
                HudColors.PREVIEW);
    }

    /**
     * Trajectoire d'une poussée : de la case visée vers son arrivée.
     *
     * <p>Quand l'arrivée est hors grille, le trait s'arrête sur un bloc posé au bord — le mur. C'est
     * le cas le plus rentable du jeu et le moins calculable d'un coup d'œil : le montrer était l'une
     * des raisons d'écrire ce jalon.
     */
    private void drawPushTrack(TilePreview preview, boolean committed) {
        int step = preview.direction().step();
        int start = cellCentre(preview.aim());
        boolean onBoard = arena.grid().contains(preview.landing());
        int end = onBoard
                ? cellCentre(preview.landing())
                : layout.cellLeft(preview.aim()) + (step > 0 ? ArenaLayout.CELL_WIDTH + 2 : -3);

        painter.fill(Math.min(start, end), ArenaLayout.PREVIEW_Y + 1, Math.abs(end - start), 1,
                HudColors.PREVIEW);
        if (preview.outcome() == ActionResult.COLLIDED) {
            // Un bloc épais là où ça butte : la poussée s'arrête ici et fait mal des deux côtés.
            painter.fill(end - (step > 0 ? 0 : 2), ArenaLayout.PREVIEW_Y - 1, 3,
                    ArenaLayout.PREVIEW_HEIGHT + 2, HudColors.PREVIEW);
        } else {
            drawReachBar(preview.landing(), HudColors.PREVIEW, committed);
        }
    }

    private int cellCentre(int cell) {
        return layout.cellLeft(cell) + ArenaLayout.CELL_WIDTH / 2;
    }

    // ------------------------------------------------------------------ texte de l'interface

    /**
     * Bandeau d'état, en haut de la zone garantie.
     *
     * <p>Il ne porte que ce que la scène ne peut pas montrer : le numéro de vague, le compte des
     * tours, et le rappel de la touche d'aide. Les points de vie y figurent en chiffres parce que
     * les pastilles au-dessus des têtes répondent à « est-ce que ça va » et pas à « combien
     * exactement », et que la différence décide parfois du tour.
     */
    private void drawBanner() {
        int left = safeLeft() + 4;
        int width = context.viewport().getSafeWorldWidth() - 8;

        String hint = HudText.HELP_HINT;
        int hintWidth = PixelFont.widthOf(hint, 1);
        drawLine(HudText.banner(arena), left, HudLayout.BANNER_TOP, HudColors.TEXT,
                width - hintWidth - 6);
        drawLine(hint, left + width - hintWidth, HudLayout.BANNER_TOP, HudColors.TEXT_DIM, hintWidth);
    }

    /**
     * Panneau d'information : ce que fera le sommet de la file, ou le détail de la tuile survolée.
     *
     * <p>Une seule bande pour les deux, ancrée au même endroit — voir {@link HudLayout#INFO_TOP}.
     */
    private void drawInfoPanel() {
        if (helpVisible) {
            // L'aide est modale : deux panneaux superposés se lisent comme un défaut d'affichage,
            // et de toute façon l'aide couvre la bande d'information.
            return;
        }
        drawPanel(infoLines(), safeLeft() + 4, HudLayout.INFO_TOP);
    }

    private List<String> infoLines() {
        int rack = hoveredRackSlot;
        if (rack >= 0 && rack < arena.rack().tiles().size()) {
            return HudText.rackTooltip(arena, arena.rack().tiles().get(rack));
        }
        List<Tile> queued = arena.queue().fromOldest();
        if (hoveredQueueSlot >= 0 && hoveredQueueSlot < queued.size()) {
            return HudText.queueTooltip(queued, hoveredQueueSlot);
        }
        if (lastResult == null) {
            return List.of(HudText.announce(arena));
        }
        return List.of(HudText.announce(arena), HudText.lastAction(lastResult));
    }

    /** Bannière de fin de partie, au centre du plateau. */
    private void drawOutcome() {
        if (!arena.isOver()) {
            return;
        }
        drawCentredPanel(HudText.outcome(arena),
                ArenaLayout.FIGURE_Y + ArenaLayout.FIGURE_HEIGHT / 2);
    }

    /** Aide : les commandes, en français, ouverte tant que le joueur n'a rien fait. */
    private void drawHelp() {
        if (helpVisible) {
            drawCentredPanel(HudText.help(), StarfallGame.MIN_WORLD_HEIGHT / 2);
        }
    }

    // ------------------------------------------------------------------ primitives de panneau

    private int safeLeft() {
        return context.viewport().getSafeLeft();
    }

    /** Largeur utile pour du texte : la zone garantie, moins les marges. */
    private int textWidth() {
        return context.viewport().getSafeWorldWidth() - 8;
    }

    /**
     * Dessine une ligne, tronquée à la largeur disponible.
     *
     * <p>La troncature est un filet de sécurité, pas une mise en page : les libellés sont écrits
     * pour tenir. Mais un libellé qui déborderait sortirait de la zone garantie, c'est-à-dire
     * disparaîtrait sur certaines fenêtres et pas sur d'autres — le genre de défaut qu'on ne voit
     * jamais sur sa propre machine.
     */
    private void drawLine(String line, int x, int top, Color color, int maxWidth) {
        context.batch().setColor(color);
        font.draw(context.batch(), truncate(line, maxWidth), x, top, 1);
        context.batch().setColor(Color.WHITE);
    }

    static String truncate(String line, int maxWidth) {
        String result = line;
        while (!result.isEmpty() && PixelFont.widthOf(result, 1) > maxWidth) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    /** Panneau ancré par son coin haut-gauche, qui grandit vers le bas. */
    private void drawPanel(List<String> lines, int left, int top) {
        int maxWidth = textWidth() - 2 * HudLayout.PANEL_PADDING;
        List<String> fitted = new ArrayList<>(lines.size());
        int widest = 0;
        for (String line : lines) {
            String clipped = truncate(line, maxWidth);
            fitted.add(clipped);
            widest = Math.max(widest, PixelFont.widthOf(clipped, 1));
        }

        int width = widest + 2 * HudLayout.PANEL_PADDING;
        int height = HudLayout.panelHeight(fitted.size());
        painter.fill(left, top - height, width, height, HudColors.PANEL);
        painter.outline(left, top - height, width, height, HudColors.PANEL_EDGE);

        int y = top - HudLayout.PANEL_PADDING;
        for (int i = 0; i < fitted.size(); i++) {
            drawLine(fitted.get(i), left + HudLayout.PANEL_PADDING, y,
                    i == 0 ? HudColors.TEXT : HudColors.TEXT_DIM, maxWidth);
            y -= HudLayout.TEXT_STEP;
        }
    }

    /** Panneau centré sur la zone garantie, autour d'une ordonnée donnée. */
    private void drawCentredPanel(List<String> lines, int centreY) {
        int maxWidth = textWidth() - 2 * HudLayout.PANEL_PADDING;
        int widest = 0;
        for (String line : lines) {
            widest = Math.max(widest, PixelFont.widthOf(truncate(line, maxWidth), 1));
        }
        int width = widest + 2 * HudLayout.PANEL_PADDING;
        int height = HudLayout.panelHeight(lines.size());
        int left = safeLeft() + (context.viewport().getSafeWorldWidth() - width) / 2;
        drawPanel(lines, left, centreY + height / 2);
    }

    // ------------------------------------------------------------------ plateau

    /**
     * Trois bandes horizontales seulement : le ciel, le mur derrière la grille, et le vide sous
     * elle. Le décor n'a rien à raconter tant que la lecture tactique n'est pas nette — et une
     * bande unie sous les dalles donne aux repères un fond sur lequel ils tranchent.
     *
     * <p>Les bandes couvrent <b>toute la zone dessinée</b>, pas seulement les 320x180 garantis.
     * Peindre uniquement la zone garantie laissait, sur toute fenêtre qui n'est pas en 16:9, des
     * bandes de couleur d'effacement à arête franche autour de l'arène : à 400x240, un îlot cerné
     * de noir qui ressemble à un défaut d'affichage plus qu'à un décor.
     */
    private void drawBackdrop() {
        var viewport = context.viewport();
        int left = viewport.getDrawnLeft();
        int bottom = viewport.getDrawnBottom();
        int width = viewport.getDrawnWorldWidth();
        int height = viewport.getDrawnWorldHeight();

        painter.fill(left, bottom, width, height, SKY);
        painter.fill(left, ArenaLayout.GROUND_Y + ArenaLayout.GROUND_HEIGHT, width, 48, WALL);
        painter.fill(left, bottom, width, ArenaLayout.GROUND_Y - bottom, PIT);
    }

    private void drawGround() {
        var ground = context.atlas().region("ground/plain");
        for (int cell = 0; cell < layout.gridWidth(); cell++) {
            painter.sprite(ground, layout.cellLeft(cell), ArenaLayout.GROUND_Y);
            // Un trait d'un pixel entre deux cases : sans lui, une rangée de dalles identiques ne
            // laisse pas compter les cases, et compter les cases est le geste de base du jeu.
            if (cell > 0) {
                painter.fill(layout.cellLeft(cell), ArenaLayout.GROUND_Y,
                        1, ArenaLayout.GROUND_HEIGHT, CELL_LINE);
            }
        }
    }

    /**
     * Points de vie, en pastilles sous chaque figure.
     *
     * <p>Des pastilles et non une barre : à cette échelle, compter trois carrés est plus rapide que
     * mesurer une longueur, et la différence entre « il lui en reste deux » et « il lui en reste
     * trois » est exactement la décision que le joueur doit prendre.
     */
    private void drawHealth(int cell, int health, int maxHealth, Color color) {
        int left = layout.cellLeft(cell) + (ArenaLayout.CELL_WIDTH - (maxHealth * 3 - 1)) / 2;
        for (int point = 0; point < maxHealth; point++) {
            painter.fill(left + point * 3, ArenaLayout.HEALTH_Y, 2, ArenaLayout.HEALTH_HEIGHT,
                    point < health ? color : HudColors.DIMMED);
        }
    }

    private void drawOccupants() {
        for (int cell : arena.grid().occupiedCells()) {
            Occupant occupant = arena.grid().occupantAt(cell);
            var region = context.atlas().region(occupant.spriteName());
            int x = layout.figureLeft(cell);

            // Chaque figure est retournée selon son orientation. Le héros est dessiné tourné à
            // droite, les ennemis tournés à gauche : ils arrivent face à lui.
            boolean flip = occupant == arena.hero()
                    ? arena.hero().facing() == Direction.LEFT
                    : occupant instanceof Enemy enemy && enemy.facing() == Direction.RIGHT;
            if (flip) {
                painter.spriteFlipped(region, x, ArenaLayout.FIGURE_Y);
            } else {
                painter.sprite(region, x, ArenaLayout.FIGURE_Y);
            }

            if (occupant == arena.hero()) {
                drawHealth(cell, arena.hero().health(), Hero.MAX_HEALTH, HERO_MARK);
            } else if (occupant instanceof Enemy enemy) {
                drawHealth(cell, enemy.health(), enemy.maxHealth(), HudColors.THREAT);
                if (enemy.has(Trait.EXPLOSIF)) {
                    // Un explosif coûte deux points de vie à qui le tue au contact, soit 40 % de la
                    // santé du héros — et son sprite est celui de son archétype, donc rien ne le
                    // distinguait. Le seul indice était une ligne de texte, l'une des premières
                    // tronquées sur une petite fenêtre.
                    painter.outline(x - 1, ArenaLayout.FIGURE_Y - 1,
                            ArenaLayout.FIGURE_WIDTH + 2, ArenaLayout.FIGURE_HEIGHT + 2,
                            HudColors.RECHARGE);
                }
            }
        }
    }

    /**
     * Les cases que les ennemis ont annoncé vouloir frapper.
     *
     * <p>C'est la réponse visuelle à « qui va frapper quoi, où ». Elle est peinte <b>sur la dalle</b>
     * et non en marge : la question du joueur est « est-ce que je peux rester là », et la réponse
     * doit être au même endroit que la question.
     */
    private void drawThreats() {
        for (int cell = 0; cell < layout.gridWidth(); cell++) {
            int blows = arena.threatCount(cell);
            if (blows == 0) {
                continue;
            }
            painter.outline(layout.cellLeft(cell), ArenaLayout.GROUND_Y,
                    ArenaLayout.CELL_WIDTH, ArenaLayout.GROUND_HEIGHT, HudColors.THREAT);
            // Autant de barres que de coups qui tomberont : « danger » et « deux fois plus de
            // danger » ne doivent pas se ressembler.
            for (int blow = 0; blow < blows && blow < 3; blow++) {
                painter.fill(layout.cellLeft(cell) + 2 + blow * 6,
                        ArenaLayout.GROUND_Y + ArenaLayout.GROUND_HEIGHT - 3, 4, 2, HudColors.THREAT);
            }
        }
    }

    /**
     * L'intention de chaque ennemi, au-dessus de sa tête.
     *
     * <p>La case menacée dit <em>où</em> ; ce glyphe dit <em>quoi</em>, et surtout il existe pour les
     * intentions qui ne menacent aucune case — avancer, prendre son élan — sans lesquelles le joueur
     * ne saurait pas distinguer un ennemi qui se rapproche d'un ennemi qui attend.
     */
    private void drawIntentions() {
        for (Enemy enemy : arena.enemies()) {
            int cell = arena.grid().indexOf(enemy);
            int centre = cellCentre(cell);
            int y = ArenaLayout.INTENT_Y;
            Intention intention = enemy.intention();

            switch (intention.kind()) {
                // Pointe pleine : un coup part sur une case précise.
                case ATTACK -> drawSpike(centre, y, directionTo(cell, intention.targetCell()),
                        HudColors.THREAT);
                // Double pointe : il vient de loin, et il vient sur toi.
                case CHARGE -> {
                    int step = directionTo(cell, intention.targetCell());
                    drawSpike(centre - step * 3, y, step, HudColors.THREAT);
                    drawSpike(centre + step, y, step, HudColors.THREAT);
                }
                // Deux barres : il se charge, on a un tour pour réagir.
                case WIND_UP -> {
                    painter.fill(centre - 3, y, 2, ArenaLayout.INTENT_HEIGHT, HudColors.THREAT);
                    painter.fill(centre + 2, y, 2, ArenaLayout.INTENT_HEIGHT, HudColors.THREAT);
                }
                // Chevron creux : il se déplace, il ne frappe pas. La forme diffère de la pointe
                // pleine, pour que la lecture ne repose pas seulement sur la couleur.
                case ADVANCE -> drawChevron(centre, y, directionTo(cell, intention.targetCell()),
                        HudColors.SLOT_EMPTY);
                case WAIT -> painter.fill(centre - 2, y + 2, 5, 1, HudColors.SLOT_EMPTY);
            }
        }
    }

    /**
     * Sens d'un repère, déduit de la case visée et non de l'orientation de l'ennemi.
     *
     * <p>Un archer qui recule reste tourné vers le héros : la flèche suivait son regard et disait
     * donc « il vient sur moi » alors qu'il s'éloignait.
     */
    private static int directionTo(int from, int target) {
        return target < 0 ? 1 : Integer.signum(target - from);
    }

    /**
     * Pointe pleine, tournée vers {@code step}.
     *
     * <p>Elle s'<b>affine</b> vers la cible. La première version s'élargissait dans le sens de la
     * marche, ce qui la faisait lire à l'envers : un ennemi à droite du héros semblait viser la
     * droite.
     */
    private void drawSpike(int centre, int y, int step, Color color) {
        painter.fill(centre - step * 3, y + 2, 3, 2, color);
        for (int i = 0; i < 3; i++) {
            int height = 6 - 2 * i;
            painter.fill(centre + step * i, y + (6 - height) / 2, 1, height, color);
        }
    }

    /** Chevron creux, pointe tournée vers {@code step}. */
    private void drawChevron(int centre, int y, int step, Color color) {
        for (int i = 0; i < 3; i++) {
            painter.fill(centre + step * (1 - i), y + 2 - i, 1, 1, color);
            painter.fill(centre + step * (1 - i), y + 2 + i, 1, 1, color);
        }
    }

    /**
     * Tous les repères tactiques vivent sur une seule bande, juste sous les dalles.
     *
     * <p>C'est le choix qui décide de la lisibilité de la scène : rassemblés sur une ligne, le
     * repère du héros, le trait de liaison et la pointe sur la cible se lisent comme <b>une seule
     * phrase</b> — « moi, jusqu'à là ». Dispersés — un repère sous les pieds, une ligne au-dessus
     * des têtes — ils obligeaient l'œil à faire trois allers-retours et à compter les cases.
     */
    private void drawTacticalMarks() {
        int hero = arena.heroCell();
        int target = arena.swapTarget();

        if (target >= 0) {
            drawAimLink(hero, target);
            drawCellMark(target, TARGET_MARK);
        }
        drawHeroMark(hero);
        if (hoveredCell >= 0 && hoveredCell != hero) {
            painter.outline(layout.cellLeft(hoveredCell), ArenaLayout.GROUND_Y,
                    ArenaLayout.CELL_WIDTH, ArenaLayout.GROUND_HEIGHT, HOVER_MARK);
        }
    }

    /** Repère de case : un trait épais sous la dalle, qui ne cache jamais la figure. */
    private void drawCellMark(int cell, Color color) {
        painter.fill(layout.cellLeft(cell) + 2, ArenaLayout.MARK_Y, ArenaLayout.CELL_WIDTH - 4, 2, color);
    }

    /** Trait de liaison entre le héros et la cible, sur la même ligne que les deux repères. */
    private void drawAimLink(int hero, int target) {
        int heroCentre = cellCentre(hero);
        int targetCentre = cellCentre(target);

        painter.fill(Math.min(heroCentre, targetCentre), ArenaLayout.MARK_Y,
                Math.abs(targetCentre - heroCentre), 1, TARGET_MARK);
    }

    /**
     * Repère du héros : un trait terminé par une pointe du côté qu'il regarde.
     *
     * <p>Position et orientation sont dites par une seule forme, et non par deux symboles côte à
     * côte. Séparés, ils se télescopaient avec le trait de liaison et l'œil devait démêler trois
     * signes au même endroit ; fondus, ils se lisent d'un coup — et la pointe indique déjà le sens
     * de l'échange, ce qui rend inutile une seconde flèche sur la cible.
     */
    private void drawHeroMark(int cell) {
        int step = arena.hero().facing().step();
        int left = layout.cellLeft(cell) + 2;
        int width = ArenaLayout.CELL_WIDTH - 4;
        painter.fill(left, ArenaLayout.MARK_Y, width, 2, HERO_MARK);

        // Pointe : quatre colonnes qui s'affinent vers l'extérieur, centrées sur le trait.
        int tip = step > 0 ? left + width : left - 1;
        for (int i = 0; i < 4; i++) {
            int height = 8 - 2 * i;
            painter.fill(tip + step * i, ArenaLayout.MARK_Y + 1 - height / 2, 1, height, HERO_MARK);
        }
    }

    // ------------------------------------------------------------------ file et râtelier

    /**
     * La file d'actions.
     *
     * <p>Elle se lit de gauche à droite dans l'ordre où l'on a posé — mais elle s'exécute à
     * l'envers. C'est le point le plus contre-intuitif du jeu, donc la <b>flèche de retour</b>
     * au-dessus de la dernière tuile posée n'est pas décorative : elle dit « c'est celle-là qui
     * part la première ».
     */
    private void drawQueue() {
        var empty = context.atlas().region("tile/empty");
        List<Tile> tiles = arena.queue().fromOldest();

        for (int slot = 0; slot < ActionQueue.CAPACITY; slot++) {
            int x = hud.queueSlotX(slot);
            if (slot < tiles.size()) {
                painter.sprite(context.atlas().region(tiles.get(slot).spriteName()), x, HudLayout.QUEUE_Y);
                if (hoveredQueueSlot == slot) {
                    painter.outline(x - 1, HudLayout.QUEUE_Y - 1,
                            HudLayout.TILE_SIZE + 2, HudLayout.TILE_SIZE + 2, HOVER_MARK);
                }
            } else {
                // Un emplacement vide doit rester comptable d'un coup d'œil : la file en a cinq, et
                // savoir combien il en reste fait partie de la décision. Le contour est là pour ça.
                painter.spriteTinted(empty, x, HudLayout.QUEUE_Y, HudColors.SLOT_EMPTY);
                painter.outline(x, HudLayout.QUEUE_Y, HudLayout.TILE_SIZE, HudLayout.TILE_SIZE,
                        HudColors.SLOT_OUTLINE);
            }
        }

        if (!tiles.isEmpty()) {
            drawNextMarker(hud.queueSlotX(tiles.size() - 1));
        }
    }

    /**
     * Repère de la prochaine tuile exécutée.
     *
     * <p>Trois signes concordants, parce que l'ordre à l'envers est le point le plus
     * contre-intuitif du jeu : un cadre autour de la tuile, un trait plein sous elle, et un chevron
     * <b>pointant vers elle</b>. La version précédente n'avait qu'un chevron, orienté à l'envers et
     * posé dans la bande du râtelier, où les repères de celui-ci l'effaçaient en partie : il se
     * lisait comme une annotation du râtelier.
     */
    private void drawNextMarker(int slotX) {
        painter.outline(slotX - 1, HudLayout.QUEUE_Y - 1,
                HudLayout.TILE_SIZE + 2, HudLayout.TILE_SIZE + 2, HudColors.QUEUE);
        painter.fill(slotX, HudLayout.QUEUE_MARK_BOTTOM, HudLayout.TILE_SIZE, 2, HudColors.QUEUE);

        // Chevron pointe en bas : la pointe touche la tuile qu'il désigne.
        int centre = slotX + HudLayout.TILE_SIZE / 2;
        int apex = HudLayout.QUEUE_Y + HudLayout.TILE_SIZE + 1;
        for (int i = 0; i < 4; i++) {
            painter.fill(centre - i, apex + i, 1, 1, HudColors.QUEUE);
            painter.fill(centre + i, apex + i, 1, 1, HudColors.QUEUE);
        }
    }

    /**
     * Le râtelier : ce que le héros sait faire, et ce qui lui est accessible <em>maintenant</em>.
     *
     * <p>Trois états, et chacun se distingue sans lire de texte :
     * <ul>
     *   <li><b>disponible</b> — pleine lumière, aucun repère ;</li>
     *   <li><b>posée sur la file</b> — éteinte, avec un trait or, la couleur de la file : « elle
     *       est là-bas » ;</li>
     *   <li><b>en recharge</b> — éteinte, avec autant de points ocre que de tours à attendre.</li>
     * </ul>
     *
     * <p>Il n'y a délibérément <b>aucun repère de disponibilité</b> : la pleine lumière suffit, et
     * le trait vert qui jouait ce rôle utilisait exactement le vert des tuiles Free-Play. Au repos,
     * les six tuiles portaient donc du vert — alors que le vert doit vouloir dire une seule chose.
     *
     * <p>Faire disparaître une tuile indisponible serait plus simple à dessiner et beaucoup moins
     * utile : le joueur doit voir <em>laquelle</em> lui manque. L'infobulle dit le reste — combien
     * de points il reste à attendre, et ce que la tuile fera quand elle reviendra.
     */
    private void drawRack() {
        List<Tile> tiles = arena.rack().tiles();
        for (int i = 0; i < tiles.size(); i++) {
            Tile tile = tiles.get(i);
            int x = hud.rackSlotX(i);
            var region = context.atlas().region(tile.spriteName());

            if (arena.rack().isReady(tile)) {
                painter.sprite(region, x, HudLayout.RACK_Y);
            } else {
                painter.spriteTinted(region, x, HudLayout.RACK_Y, HudColors.DIMMED);
            }

            int missing = arena.rack().missingPoints(tile);
            if (missing > 0) {
                for (int point = 0; point < missing; point++) {
                    painter.fill(x + 2 + point * 3, HudLayout.RACK_MARK_BOTTOM, 2, 2,
                            HudColors.RECHARGE);
                }
            } else if (!arena.rack().holds(tile)) {
                painter.fill(x, HudLayout.RACK_MARK_BOTTOM, HudLayout.TILE_SIZE, 2, HudColors.QUEUE);
            }

            if (hoveredRackSlot == i) {
                painter.outline(x - 1, HudLayout.RACK_Y - 1,
                        HudLayout.TILE_SIZE + 2, HudLayout.TILE_SIZE + 2, HOVER_MARK);
            }
        }
    }

    // ------------------------------------------------------------------ cadrage

    /**
     * La caméra reste calée sur le centre de la grille, quelle que soit sa largeur. La valeur vient
     * de {@link ArenaLayout} et non d'une constante recopiée : sans cela, le couplage entre la
     * caméra et la grille ne reposait que sur deux constantes qui se trouvaient coïncider.
     */
    @Override
    public float cameraTargetX() {
        return layout.cameraTargetX();
    }

    @Override
    public float cameraTargetY() {
        return StarfallGame.MIN_WORLD_HEIGHT / 2f;
    }

    @Override
    public int contentTopWorldY() {
        // Le bandeau d'état occupe désormais le haut de la zone garantie : plus rien ne peut se
        // poser au-dessus.
        return HudLayout.BANNER_TOP;
    }

    /**
     * Aucune ligne de diagnostic.
     *
     * <p>L'interface du jeu vit en pixels-monde depuis ce jalon. Laisser en plus un bandeau écran
     * qui répète le même état aurait donné deux affichages à tenir d'accord — et l'expérience de ce
     * projet est sans appel sur ce point : <b>ce qui est écrit à deux endroits finit par
     * diverger</b>. La mire de calibration, elle, garde son bandeau : c'est un instrument, pas un
     * jeu.
     */
    @Override
    public List<String> overlayLines(int screenWidth, int screenHeight) {
        return List.of();
    }
}
