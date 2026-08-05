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

    /**
     * Le scénario rejoué en mode capture.
     *
     * <p>Il était codé en dur sur la ligne gagnante. Une review a montré que quatre états
     * d'interface ne pouvaient être atteints par elle — la bannière de défaite, une poussée qui
     * bute, une case non cliquable survolée, la reprise d'une tuile — et qu'on pouvait donc les
     * casser sans qu'aucune planche ne bronche. La scène {@code showcase} les joue.
     */
    private final CaptureScenario scenario;

    /** La scène du jeu, rejouant la ligne gagnante en mode capture. */
    public ArenaScene() {
        this(CaptureScript.SCENARIO);
    }

    /** La même scène, sur un autre scénario. Voir {@link ShowcaseScript}. */
    public ArenaScene(CaptureScenario scenario) {
        this.scenario = scenario;
    }
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
        arena = ArenaSetup.trainingArena(gridWidth, context.options().startWave);
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
        if (helpVisible) {
            // L'aide est <b>modale</b>. Elle recouvre le plateau et la moitie du ratelier ; sans
            // cela, le premier clic d'une partie tombait sur une case cachee par le panneau et
            // jouait un tour entier a l'aveugle - phase ennemie comprise. Le geste ferme l'aide,
            // et rien d'autre.
            if (anyInput()) {
                helpVisible = false;
            }
            trackPointer();
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
        hoveredCell = scenario.cellAt(frameIndex);
        hoveredQueueSlot = scenario.queueSlotAt(frameIndex);
        hoveredRackSlot = scenario.rackSlotAt(frameIndex);
        helpVisible = frameIndex == 0;

        if (frameIndex == scriptedFrame) {
            return;
        }
        arena = ArenaSetup.trainingArena(layout.gridWidth(), context.options().startWave);

        ActionResult replayed = scenario.replayInto(arena, frameIndex);
        if (replayed != null) {
            lastResult = replayed;
        }
        if (frameIndex > scenario.size() && !exhaustionReported) {
            // Le dire plutôt que de laisser un relecteur croire que deux images identiques
            // signalent un rendu figé.
            System.out.println("[Starfall] le scénario de capture compte " + scenario.size()
                    + " actions ; les images suivantes répètent l'état final");
            exhaustionReported = true;
        }
        scriptedFrame = frameIndex;
    }

    private int scriptedFrame = -1;
    private boolean exhaustionReported;

    /**
     * @return vrai si une action a été déclenchée ; la souris et le clavier se partagent la règle
     *         « une action par image »
     */
    /** Vrai si le joueur a fait un geste, quel qu'il soit. Sert a refermer l'aide. */
    private static boolean anyInput() {
        return Gdx.input.justTouched() || Gdx.input.isKeyJustPressed(Input.Keys.ANY_KEY);
    }

    /** Met a jour ce qui est survole, sans rien declencher. */
    private void trackPointer() {
        Vector3 world = context.viewport().unproject(
                new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0f));
        hoveredCell = layout.cellAt(world.x, world.y);
        hoveredQueueSlot = hud.queueSlotAt(world.x, world.y);
        hoveredRackSlot = hud.rackSlotAt(world.x, world.y);
    }

    private boolean readMouse() {
        trackPointer();

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
            applied(arena.unleash());
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
        // Le sommet d'abord, toujours : survoler une tuile du ratelier effacait le preavis de
        // celle qui allait partir, or l'instant qui suit une pose est justement celui ou le
        // curseur se trouve sur le ratelier.
        TilePreview committed = arena.previewTop();
        if (committed != null) {
            drawPreview(committed, true, HudColors.PREVIEW);
        }

        Tile hovered = HudText.hoveredTile(arena, hoveredRackSlot, hoveredQueueSlot);
        if (hovered == null) {
            return;
        }
        // Ici vivait un retour anticipé « c'est déjà le préavis résolu qu'on vient de dessiner »,
        // gardé par HudText.hoveringTheTop. Il ne pouvait RIEN changer à l'image, et c'est
        // démontrable plutôt que constaté :
        //
        //   - hoveringTheTop n'est vrai que si un emplacement de FILE est survolé ;
        //   - la ligne ci-dessous force alors « available » à vrai, donc la couleur à PREVIEW ;
        //   - drawStaticReach dessine un CONTOUR, et le préavis résolu du sommet a déjà rempli ou
        //     contouré le même rectangle dans la même couleur.
        //
        // Un contour de la couleur d'un aplat de la même couleur ne se voit pas. La review avait
        // repéré que la planche choisie pour l'exercer était neutre ; le fond est pire, aucune
        // planche ne pouvait l'exercer. Retiré plutôt que rafistolé, comme les seuils avant lui.
        //
        // Ce que le javadoc de hoveringTheTop décrivait — « le préavis du sommet passait de plein à
        // creux » — est empêché par la couleur, pas par ce retour.
        boolean available = arena.rack().isReady(hovered) || hoveredQueueSlot >= 0;
        drawStaticReach(hovered, available ? HudColors.PREVIEW : HudColors.SLOT_EMPTY);
    }

    /**
     * Portée <b>statique</b> d'une tuile survolée : les cases qu'elle peut atteindre, sans prétendre
     * savoir ce qu'elle y trouvera.
     *
     * <p>Elle est revenue à sa place ici, et c'est une conséquence directe de la nouvelle économie.
     * Tant que poser était gratuit, montrer le préavis <em>résolu</em> d'une tuile du râtelier était
     * exact : rien ne bougeait entre le survol et le moment où elle pouvait agir. Depuis que poser
     * consomme un tour, les ennemis jouent <b>entre les deux</b> — et la review a mesuré que
     * l'annonce se trompait alors dans <b>un quart</b> des cas : « aucune cible » puis une frappe
     * qui porte, un élan annoncé puis bloqué.
     *
     * <p>C'est exactement l'invariant de M8, et il vaut toujours : seul le sommet de la file
     * s'exécutera contre le plateau affiché, donc seul lui a le droit d'annoncer un résultat. Le
     * tableau {@code Tile.reach()}, que la review de M8 avait trouvé inutilisé, reprend ici du
     * service — pour la raison même qui l'avait fait écrire.
     */
    private void drawStaticReach(Tile tile, Color color) {
        int hero = arena.heroCell();
        int step = arena.hero().facing().step();
        for (int offset : tile.reach()) {
            int cell = hero + offset * step;
            if (arena.grid().contains(cell)) {
                painter.outline(layout.insetLeft(cell), ArenaLayout.PREVIEW_Y,
                        ArenaLayout.insetWidth(), ArenaLayout.PREVIEW_HEIGHT, color);
            }
        }
    }

    /**
     * Dessine un préavis. {@code committed} distingue les deux régimes : plein pour ce qui va se
     * produire, creux pour ce que la tuile survolée pourrait atteindre.
     */
    private void drawPreview(TilePreview preview, boolean committed, Color color) {
        switch (preview.kind()) {
            case HIT -> {
                drawReachLink(arena.heroCell(), preview.aim(), committed, color);
                drawReachBar(preview.aim(), color, committed);
            }
            case PUSH -> {
                drawReachBar(preview.aim(), color, committed);
                drawPushTrack(preview, committed, color);
            }
            case MOVE -> {
                drawReachLink(arena.heroCell(), preview.landing(), committed, color);
                drawReachBar(preview.landing(), color, committed);
            }
            // Le demi-tour ne vise aucune case : un repère sous le héros, et la ligne d'annonce dit
            // vers où il regardera.
            case TURN -> drawReachBar(arena.heroCell(), color, committed);
            // Une tuile qui ne portera pas mérite d'être annoncée : elle sera dépensée quand même.
            case NONE -> {
                if (arena.grid().contains(preview.aim())) {
                    drawMissMark(preview.aim(), color);
                }
            }
        }
    }

    private void drawReachBar(int cell, Color color, boolean committed) {
        int x = layout.insetLeft(cell);
        int width = ArenaLayout.insetWidth();
        if (committed) {
            painter.fill(x, ArenaLayout.PREVIEW_Y, width, ArenaLayout.PREVIEW_HEIGHT, color);
        } else {
            painter.outline(x, ArenaLayout.PREVIEW_Y, width, ArenaLayout.PREVIEW_HEIGHT, color);
        }
    }

    /**
     * Repère d'un coup qui ne portera pas : un cadre barré d'une croix.
     *
     * <p>Il était d'abord dessiné en {@link HudColors#DIMMED}, c'est-à-dire dans la couleur qui dit
     * « absent ». Deux conséquences, toutes deux mauvaises : cette couleur portait alors trois sens
     * différents, et surtout elle disparaissait — un rectangle de trois pixels à
     * {@code 0x3a3a44} sur le fond de la fosse, pour porter la promesse-titre du jalon. Le
     * message passe maintenant par une <b>forme</b>, dans la couleur du préavis : c'est la même
     * doctrine que le chevron creux des intentions, où la lecture ne doit jamais reposer sur la
     * seule couleur.
     */
    private void drawMissMark(int cell, Color color) {
        int x = layout.insetLeft(cell);
        int width = ArenaLayout.insetWidth();
        painter.outline(x, ArenaLayout.PREVIEW_Y, width, ArenaLayout.PREVIEW_HEIGHT, color);
        for (int i = 0; i < ArenaLayout.PREVIEW_HEIGHT; i++) {
            painter.fill(x + width / 2 - 1 + i, ArenaLayout.PREVIEW_Y + i, 1, 1, color);
            painter.fill(x + width / 2 + 1 - i, ArenaLayout.PREVIEW_Y + i, 1, 1, color);
        }
    }

    /** Trait entre le héros et la case concernée, dans la bande des portées. */
    private void drawReachLink(int from, int to, boolean committed, Color color) {
        int a = Math.min(cellCentre(from), cellCentre(to));
        int b = Math.max(cellCentre(from), cellCentre(to));
        if (committed) {
            painter.fill(a, ArenaLayout.PREVIEW_Y + 1, b - a, 1, color);
            return;
        }
        // Pointillé : « plein contre creux » ne tenait que sur le repère terminal, alors que le
        // trait de liaison est ce qui occupe le plus de pixels. Une portée survolée se lisait donc
        // comme un engagement sur presque toute sa longueur.
        for (int x = a; x < b; x += 2) {
            painter.fill(x, ArenaLayout.PREVIEW_Y + 1, 1, 1, color);
        }
    }

    /**
     * Trajectoire d'une poussée : de la case visée vers son arrivée.
     *
     * <p>Quand l'arrivée est hors grille, le trait s'arrête sur un bloc posé au bord — le mur. C'est
     * le cas le plus rentable du jeu et le moins calculable d'un coup d'œil : le montrer était l'une
     * des raisons d'écrire ce jalon.
     */
    private void drawPushTrack(TilePreview preview, boolean committed, Color color) {
        int step = preview.direction().step();
        int start = cellCentre(preview.aim());
        boolean onBoard = arena.grid().contains(preview.landing());
        int end = onBoard
                ? cellCentre(preview.landing())
                : layout.cellLeft(preview.aim()) + (step > 0 ? ArenaLayout.CELL_WIDTH + 2 : -3);

        int a = Math.min(start, end);
        int b = Math.max(start, end);
        if (committed) {
            painter.fill(a, ArenaLayout.PREVIEW_Y + 1, b - a, 1, color);
        } else {
            for (int x = a; x < b; x += 2) {
                painter.fill(x, ArenaLayout.PREVIEW_Y + 1, 1, 1, color);
            }
        }

        if (preview.outcome() != ActionResult.COLLIDED) {
            drawReachBar(preview.landing(), color, committed);
            return;
        }
        // Un bloc là où ça butte : la poussée s'arrête ici et fait mal des deux côtés. Plein s'il
        // s'agit d'un engagement, creux si ce n'est qu'une portée — c'est justement le cas que le
        // jalon décrit comme « le plus rentable du jeu et le moins calculable d'un coup d'œil »,
        // donc celui où prêter un résultat à une tuile qui n'est pas au sommet trompe le plus.
        int blockX = end - (step > 0 ? 0 : 2);
        if (committed) {
            painter.fill(blockX, ArenaLayout.PREVIEW_Y - 1, 3, ArenaLayout.PREVIEW_HEIGHT + 2, color);
        } else {
            painter.outline(blockX, ArenaLayout.PREVIEW_Y - 1, 3, ArenaLayout.PREVIEW_HEIGHT + 2,
                    color);
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
        int left = hudLeft() + 4;
        int width = StarfallGame.MIN_WORLD_WIDTH - 8;

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
        drawPanel(infoLines(), hudLeft() + 4, HudLayout.INFO_TOP);
    }

    private List<String> infoLines() {
        return HudText.infoLines(arena, hoveredRackSlot, hoveredQueueSlot, hoveredCell, lastResult);
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
            // Centrée plus haut que le milieu du monde : à mi-hauteur, elle recouvrait treize des
            // seize pixels du râtelier, c'est-à-dire les six tuiles que sa deuxième ligne numérote.
            drawCentredPanel(HudText.help(), HudLayout.HELP_CENTRE_Y);
        }
    }

    // ------------------------------------------------------------------ primitives de panneau

    /**
     * Bord gauche de l'interface, en pixels-monde.
     *
     * <p>Elle est ancrée sur la bande de {@value StarfallGame#MIN_WORLD_WIDTH} px-monde centrée sur
     * la <b>caméra</b>, et non sur la zone garantie du viewport. La distinction n'existe que hors
     * du 16:9, et c'est bien là qu'elle mordait : {@code getSafeWorldWidth()} vaut
     * {@code largeurFenêtre / échelle}, donc 480 px-monde sur une fenêtre 960x360 et 426 en 21:9.
     * Ancré dessus, le bandeau se collait au bord de la <em>fenêtre</em> et s'éloignait du plateau
     * de 80 px-monde — exactement ce que le javadoc de {@code HudLayout.BANNER_TOP} déclarait
     * vouloir éviter. Le plateau, lui, tient toujours dans cette bande de 320 : c'est le seul
     * repère qui l'accompagne quelle que soit la forme de la fenêtre.
     */
    private int hudLeft() {
        return layout.cameraTargetX() - StarfallGame.MIN_WORLD_WIDTH / 2;
    }

    /** Largeur utile pour du texte : la bande d'interface, moins les marges. */
    private static int textWidth() {
        return StarfallGame.MIN_WORLD_WIDTH - 8;
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
        int left = hudLeft() + (StarfallGame.MIN_WORLD_WIDTH - width) / 2;
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

        painter.fill(left, bottom, width, height, HudColors.SKY);
        painter.fill(left, ArenaLayout.GROUND_Y + ArenaLayout.GROUND_HEIGHT, width, 48, HudColors.WALL);
        painter.fill(left, bottom, width, ArenaLayout.GROUND_Y - bottom, HudColors.PIT);
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
            if (arena.summonsAt(cell)) {
                // Une invocation ne fait tomber aucun coup : elle a sa forme à elle, un losange
                // creux, et surtout elle ne compte pas dans les barres. Un joueur qui lit
                // « deux coups » doit en recevoir deux.
                drawDiamond(cellCentre(cell), ArenaLayout.GROUND_Y + 1, HudColors.THREAT);
            }
            int pushed = arena.announcedPushTo(cell);
            if (pushed >= 0) {
                // Où le héros atterrira s'il reste : un chevron, la forme que le jeu utilise déjà
                // pour « quelqu'un se déplace et ne frappe pas », dans le gris qui va avec. Il
                // manquait, et c'est pourtant tout le coût de la ruée — être déplacé d'une case ne
                // veut pas dire la même chose au milieu du plateau ou contre un mur.
                drawChevron(cellCentre(pushed), ArenaLayout.GROUND_Y + 1,
                        Integer.signum(pushed - cell), HudColors.SLOT_EMPTY);
            }
            int blows = arena.threatCount(cell);
            if (blows == 0) {
                continue;
            }
            painter.outline(layout.cellLeft(cell), ArenaLayout.GROUND_Y,
                    ArenaLayout.CELL_WIDTH, ArenaLayout.GROUND_HEIGHT, HudColors.THREAT);
            // Autant de barres que de coups qui tomberont : « danger » et « deux fois plus de
            // danger » ne doivent pas se ressembler. Le pas se resserre au-delà de trois plutôt que
            // de s'arrêter net : quatre coups annoncés s'affichaient « trois », et la vague 3 peut
            // en produire cinq alors que le héros n'a que cinq points de vie.
            int pitch = ArenaLayout.threatBarPitch(blows);
            int barWidth = ArenaLayout.threatBarWidth(blows);
            for (int blow = 0; blow < ArenaLayout.threatBarsDrawn(blows); blow++) {
                painter.fill(layout.insetLeft(cell) + blow * pitch,
                        ArenaLayout.GROUND_Y + ArenaLayout.GROUND_HEIGHT - 3, barWidth, 2,
                        HudColors.THREAT);
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
                // Ruée : la double pointe de la charge, doublée d'un chevron de poussée derrière la
                // cible. La forme dit les deux temps de la combinaison — il vient, puis il déplace.
                case RUSH -> {
                    // Une pointe pleine — il vient et il frappe — suivie d'un chevron creux dans le
                    // même sens : la forme du déplacement, celle qu'utilisent déjà les ennemis qui
                    // avancent sans frapper. « Il vient, puis il te déplace », en deux formes que
                    // le joueur connaît déjà séparément.
                    //
                    // La première version ajoutait à la double pointe de la charge une barre d'un
                    // pixel. Deux effets radicalement différents pour un écart d'un pixel, qui à
                    // l'échelle ×1 ressemblait de surcroît à une demi-barre de prise d'élan.
                    int step = directionTo(cell, intention.targetCell());
                    drawSpike(centre - step * 2, y, step, HudColors.THREAT);
                    drawChevron(centre + step * 4, y, step, HudColors.THREAT);
                }
                // Invocation : un losange creux, une forme qu'aucune attaque n'utilise. Rien ne
                // tombera sur cette case — mais quelqu'un s'y lèvera.
                case SUMMON -> drawDiamond(centre, y, HudColors.THREAT);
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

    /** Losange creux : l'invocation, seule intention qui ne fait tomber aucun coup. */
    private void drawDiamond(int centre, int y, Color color) {
        int middle = y + ArenaLayout.INTENT_HEIGHT / 2;
        for (int i = 0; i < 3; i++) {
            painter.fill(centre - 2 + i, middle - i, 1, 1, color);
            painter.fill(centre + 2 - i, middle - i, 1, 1, color);
            painter.fill(centre - 2 + i, middle + i, 1, 1, color);
            painter.fill(centre + 2 - i, middle + i, 1, 1, color);
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
            drawSwapTargetMark(target, hero);
        }
        drawHeroMark(hero);
        // Le surlignage s'allumait sur toutes les cases, y compris celles qu'un clic ne pouvait
        // pas atteindre - derriere un occupant, ou n'importe ou apres la fin de la partie. Le
        // pointeur promettait une action qui ne venait pas. L'objection date de M4.
        if (hoveredCell >= 0 && arena.clickable(hoveredCell)) {
            painter.outline(layout.cellLeft(hoveredCell), ArenaLayout.GROUND_Y,
                    ArenaLayout.CELL_WIDTH, ArenaLayout.GROUND_HEIGHT, HOVER_MARK);
        }
    }

    /**
     * Repère de la cible d'échange : un trait, et une <b>pointe qui regarde le héros</b>.
     *
     * <p>C'était un simple trait épais de deux pixels, posé sur la même ligne et dans la même
     * couleur que le trait de liaison, qui en fait un. Les deux ne se distinguaient donc que par
     * <em>un pixel d'épaisseur</em> — il fallait regarder à l'échelle ×1 pour les démêler. La review
     * du jalon de la grille l'avait relevé ; le correctif attendait qu'on sache par quoi remplacer.
     *
     * <p>La réponse est celle que le projet s'est donnée depuis, à chaque fois que deux signes se
     * ressemblaient de trop : <b>une forme vaut mieux qu'une nuance</b>. C'est ce qui distingue le
     * chevron creux de la pointe pleine chez les ennemis, le losange de l'invocation, la croix d'un
     * coup qui rate.
     *
     * <p>La pointe est tournée <em>vers le héros</em>, et pas vers la cible : elle dit « celui-ci
     * vient à toi », ce qui est exactement ce que fait un échange de place. Avec la pointe du héros
     * qui, elle, regarde la cible, les deux repères se lisent comme une seule phrase — deux flèches
     * qui se font face, c'est-à-dire un troc.
     */
    private void drawSwapTargetMark(int cell, int heroCell) {
        paint(layout.targetMark(cell, heroCell), TARGET_MARK);
    }

    /**
     * Peint un repère tactique, sans rien décider de sa forme.
     *
     * <p>C'est volontairement la méthode la plus bête du fichier. La review précédente a montré
     * pourquoi : tant que la scène calculait le <em>sens</em> des pointes, on pouvait l'inverser —
     * le héros regardant derrière lui, la cible fuyant le héros — sans qu'un seul des 448 tests ne
     * rougisse. Les tests gardaient l'arithmétique de {@link ArenaLayout}, jamais son câblage.
     *
     * <p>Il ne reste donc ici aucun retrait, aucun signe, aucune hauteur de pointe : la mise en page
     * rend des rectangles, et cette boucle les remplit.
     *
     * @param shapes rectangles à remplir, en pixels-monde
     * @param color  couleur du repère
     */
    private void paint(java.util.List<ArenaLayout.MarkShape> shapes, Color color) {
        for (ArenaLayout.MarkShape shape : shapes) {
            painter.fill(shape.x(), shape.y(), shape.width(), shape.height(), color);
        }
    }

    /**
     * Trait de liaison entre le héros et la cible.
     *
     * <p>Il est <b>pointillé</b>, et c'est la seconde moitié du correctif : un trait plein d'un
     * pixel à côté d'un trait plein de deux ne se départage qu'à la loupe. Pointillé, il se lit
     * comme ce qu'il est — un lien, et non un repère de case.
     */
    private void drawAimLink(int hero, int target) {
        if (Math.abs(target - hero) < 2) {
            // Deux cases qui se touchent n'ont rien à relier, et il n'y a de toute façon pas la
            // place : le pointillé courait alors entièrement sous les deux barres, invisible. Un
            // trait qu'on ne voit pas n'est pas un trait discret, c'est un trait qui ment sur ce
            // que le dessin contient.
            return;
        }
        int from = Math.min(cellCentre(hero), cellCentre(target));
        int to = Math.max(cellCentre(hero), cellCentre(target));

        for (int x = from; x < to; x += 2) {
            painter.fill(x, ArenaLayout.MARK_Y, 1, 1, TARGET_MARK);
        }
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
        paint(layout.heroMark(cell, arena.hero().facing()), HERO_MARK);
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
