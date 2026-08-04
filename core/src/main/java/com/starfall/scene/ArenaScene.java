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
import com.starfall.game.HudLayout;
import com.starfall.game.Intention;
import com.starfall.game.Occupant;
import com.starfall.game.Tile;
import com.starfall.render.PixelPainter;

import java.util.ArrayList;
import java.util.List;

/**
 * Le jeu : une grille linéaire, un héros qui s'y déplace et s'y oriente, et sa capacité d'échange
 * de place.
 *
 * <p>Tout ce qui est dessiné ici sert à répondre d'un coup d'œil aux trois questions du jalon :
 * <b>où suis-je, où est-ce que je regarde, et qu'est-ce que ma capacité viserait</b>. Le reste —
 * décor, atmosphère — n'existe pas encore et n'a pas à exister avant que ces trois réponses soient
 * nettes.
 */
public final class ArenaScene implements Scene {

    private static final Color SKY = new Color(0x141a2eff);
    private static final Color WALL = new Color(0x1c2440ff);
    private static final Color PIT = new Color(0x0c101eff);
    private static final Color CELL_LINE = new Color(0x39456bff);
    private static final Color HERO_MARK = new Color(0x54d6ffff);
    private static final Color TARGET_MARK = new Color(0xffcc33ff);
    private static final Color HOVER_MARK = HudColors.HOVER;

    /** Hauteur, sous les dalles, de la bande où vivent tous les repères tactiques. */
    private static final int MARK_Y = ArenaLayout.GROUND_Y - 4;

    private SceneContext context;
    private PixelPainter painter;
    private Arena arena;
    private ArenaLayout layout;
    private HudLayout hud;

    private int hoveredCell = -1;
    private int hoveredQueueSlot = -1;
    private int hoveredRackSlot = -1;
    private ActionResult lastResult;

    @Override
    public String name() {
        return "arena";
    }

    @Override
    public void create(SceneContext context) {
        this.context = context;
        this.painter = context.painter();

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
     * contact montre l'échange de place à l'œuvre au lieu de trois copies de la position de départ.
     */
    @Override
    public void act(float time, int frameIndex, boolean interactive) {
        if (!interactive) {
            hoveredCell = -1;
            replayScript(frameIndex);
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
     * Rejoue les {@code frameIndex} premières actions du scénario depuis un état neuf.
     *
     * <p>Rejouer depuis le début plutôt qu'appliquer une action de plus est ce qui garde la capture
     * reproductible : {@code act} est appelée à chaque image rendue, pas une fois par image écrite.
     */
    private void replayScript(int frameIndex) {
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
     * Le scénario montre la file d'actions, parce que c'est ce que le jalon apporte : on charge, on
     * regarde la file se remplir, puis on la dépile — et l'on voit qu'elle se vide à l'envers de
     * l'ordre où on l'a remplie.
     */
    private static final ScriptedAction[] SCRIPT = {
            a -> a.queueTile(Tile.STRIKE),   // on charge, gratuitement : le compteur reste à zéro
            a -> a.queueTile(Tile.THRUST),   // et les ennemis n'avancent pas pendant ce temps
            a -> a.executeTop(),             // l'estoc, posé en dernier, part en premier
            a -> a.executeTop(),             // puis la frappe — et les intentions ont changé
            a -> a.step(Direction.LEFT),     // demi-tour : un tour de plus pour les ennemis
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
            lastResult = arena.queueTile(arena.rack().tiles().get(hoveredRackSlot));
            return true;
        }
        if (hoveredQueueSlot >= 0) {
            lastResult = arena.unqueueAt(hoveredQueueSlot);
            return true;
        }
        if (hoveredCell >= 0) {
            lastResult = arena.clickOn(hoveredCell);
            return true;
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
            lastResult = arena.step(Direction.LEFT);
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.RIGHT)
                || Gdx.input.isKeyJustPressed(Input.Keys.D)) {
            lastResult = arena.step(Direction.RIGHT);
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.E)) {
            lastResult = arena.swapWithTarget();
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)
                || Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
            lastResult = arena.executeTop();
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.BACKSPACE)
                || Gdx.input.isKeyJustPressed(Input.Keys.FORWARD_DEL)) {
            lastResult = arena.unqueueAt(arena.queue().size() - 1);
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
                lastResult = arena.queueTile(tiles.get(i));
                return;
            }
        }
    }

    @Override
    public void drawWorld() {
        drawBackdrop();
        drawGround();
        drawThreats();
        drawOccupants();
        drawIntentions();
        drawTacticalMarks();
        // Le râtelier d'abord : ses repères ne doivent jamais pouvoir recouvrir ceux de la file,
        // même si les deux bandes venaient à se croiser un jour.
        drawRack();
        drawQueue();
        painter.color(Color.WHITE);
    }

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
     * utile : le joueur doit voir <em>laquelle</em> lui manque.
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
            int centre = layout.cellLeft(cell) + ArenaLayout.CELL_WIDTH / 2;
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
                case WAIT -> {
                    painter.fill(centre - 2, y + 2, 5, 1, HudColors.SLOT_EMPTY);
                }
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
        painter.fill(layout.cellLeft(cell) + 2, MARK_Y, ArenaLayout.CELL_WIDTH - 4, 2, color);
    }

    /** Trait de liaison entre le héros et la cible, sur la même ligne que les deux repères. */
    private void drawAimLink(int hero, int target) {
        int heroCentre = layout.cellLeft(hero) + ArenaLayout.CELL_WIDTH / 2;
        int targetCentre = layout.cellLeft(target) + ArenaLayout.CELL_WIDTH / 2;

        painter.fill(Math.min(heroCentre, targetCentre), MARK_Y,
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
        painter.fill(left, MARK_Y, width, 2, HERO_MARK);

        // Pointe : quatre colonnes qui s'affinent vers l'extérieur, centrées sur le trait.
        int tip = step > 0 ? left + width : left - 1;
        for (int i = 0; i < 4; i++) {
            int height = 8 - 2 * i;
            painter.fill(tip + step * i, MARK_Y + 1 - height / 2, 1, height, HERO_MARK);
        }
    }

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
        // Les glyphes d'intention sont le contenu le plus haut, pas les têtes : c'est leur bande
        // que le bandeau d'interface ne doit jamais mordre.
        return ArenaLayout.INTENT_TOP + 1;
    }

    @Override
    public List<String> overlayLines(int screenWidth, int screenHeight) {
        List<String> lines = new ArrayList<>();
        lines.add("STARFALL - JALON M6 - ENNEMIS ET INTENTIONS");
        lines.add("TOUR " + arena.turnsTaken() + "   GRILLE : " + layout.gridWidth()
                + " CASES   HÉROS : CASE " + (arena.heroCell() + 1)
                + "   REGARD : " + arena.hero().facing().label().toUpperCase());

        Tile next = arena.queue().top();
        lines.add("FILE : " + arena.queue().size() + "/" + ActionQueue.CAPACITY
                + "   PROCHAINE : " + (next == null ? "AUCUNE"
                        : next.label().toUpperCase() + (next.isFreePlay() ? " (GRATUITE)" : "")));

        int target = arena.swapTarget();
        lines.add("ÉCHANGE : " + (target < 0 ? "AUCUNE CIBLE" : "CASE " + (target + 1)
                + " (" + arena.grid().occupantAt(target).label().toUpperCase() + ")")
                + "   TOUCHÉ : " + arena.heroHits() + " FOIS");

        StringBuilder threats = new StringBuilder();
        for (Enemy enemy : arena.enemies()) {
            if (threats.length() > 0) {
                threats.append("   ");
            }
            threats.append(enemy.label().toUpperCase()).append(" : ")
                    .append(enemy.intention().kind().label().toUpperCase());
        }
        if (threats.length() > 0) {
            lines.add(threats.toString());
        }

        if (lastResult != null) {
            lines.add("DERNIÈRE ACTION : " + lastResult.label().toUpperCase());
        }
        lines.add("1-6 : POSER   ESPACE : EXÉCUTER LE SOMMET   RETOUR ARRIÈRE : REPRENDRE");
        lines.add("FLÈCHES OU A/Q ET D : SE TOURNER PUIS AVANCER   E : ÉCHANGE   CLIC : TOUT");
        lines.add("ÉCHAP : QUITTER   F11 : PLEIN ÉCRAN");
        return lines;
    }
}
