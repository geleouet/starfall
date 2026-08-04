package com.starfall.scene;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector3;
import com.starfall.StarfallGame;
import com.starfall.game.ActionResult;
import com.starfall.game.Arena;
import com.starfall.game.ArenaLayout;
import com.starfall.game.Direction;
import com.starfall.game.Occupant;
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

    /** Occupants inertes de M4 : ils n'ont aucun comportement, ils occupent. */
    private record Dummy(String label) implements Occupant {
        @Override
        public String spriteName() {
            return "enemy/melee";
        }
    }

    private static final Color SKY = new Color(0x141a2eff);
    private static final Color WALL = new Color(0x1c2440ff);
    private static final Color PIT = new Color(0x0c101eff);
    private static final Color CELL_LINE = new Color(0x39456bff);
    private static final Color HERO_MARK = new Color(0x54d6ffff);
    private static final Color TARGET_MARK = new Color(0xffcc33ff);
    private static final Color HOVER_MARK = new Color(0x7be08aff);

    /** Hauteur, sous les dalles, de la bande où vivent tous les repères tactiques. */
    private static final int MARK_Y = ArenaLayout.GROUND_Y - 4;

    private SceneContext context;
    private PixelPainter painter;
    private Arena arena;
    private ArenaLayout layout;

    private int hoveredCell = -1;
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
        arena = new Arena(gridWidth);
        layout = new ArenaLayout(gridWidth, StarfallGame.MIN_WORLD_WIDTH / 2);

        placeDummies(gridWidth);
    }

    /**
     * Quelques occupants inertes, posés pour que la capacité d'échange ait quelque chose à viser.
     * Ils disparaîtront au profit de vrais ennemis en M6 ; leur position est calculée à partir de la
     * largeur pour rester sensée de 5 à 15 cases.
     */
    private void placeDummies(int gridWidth) {
        int hero = arena.heroCell();
        int[] wanted = {hero + 2, hero - 3, gridWidth - 1};
        int index = 1;
        for (int cell : wanted) {
            if (arena.grid().isFree(cell)) {
                arena.grid().place(cell, new Dummy("mannequin " + index++));
            }
        }
    }

    @Override
    public void act(float time, boolean interactive) {
        if (!interactive) {
            // En mode capture, aucune entrée n'est lue : les images doivent rester reproductibles.
            hoveredCell = -1;
            return;
        }
        readMouse();
        readKeyboard();
    }

    private void readMouse() {
        Vector3 world = context.viewport().unproject(
                new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0f));
        hoveredCell = layout.cellAt(world.x, world.y);

        if (Gdx.input.justTouched() && hoveredCell >= 0) {
            lastResult = arena.clickOn(hoveredCell);
        }
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
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.RIGHT) || Gdx.input.isKeyJustPressed(Input.Keys.D)) {
            lastResult = arena.step(Direction.RIGHT);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.E) || Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
            lastResult = arena.swapWithTarget();
        }
    }

    @Override
    public void drawWorld() {
        drawBackdrop();
        drawGround();
        drawOccupants();
        drawTacticalMarks();
        painter.color(Color.WHITE);
    }

    /**
     * Trois bandes horizontales seulement : le ciel, le mur derrière la grille, et le vide sous
     * elle. Le décor n'a rien à raconter tant que la lecture tactique n'est pas nette — et une
     * bande unie sous les dalles donne aux repères un fond sur lequel ils tranchent.
     */
    private void drawBackdrop() {
        painter.fill(0, 0, StarfallGame.MIN_WORLD_WIDTH, StarfallGame.MIN_WORLD_HEIGHT, SKY);
        painter.fill(0, ArenaLayout.GROUND_Y + ArenaLayout.GROUND_HEIGHT,
                StarfallGame.MIN_WORLD_WIDTH, 48, WALL);
        painter.fill(0, 0, StarfallGame.MIN_WORLD_WIDTH, ArenaLayout.GROUND_Y, PIT);
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
        for (int cell = 0; cell < layout.gridWidth(); cell++) {
            Occupant occupant = arena.grid().occupantAt(cell);
            if (occupant == null) {
                continue;
            }
            var region = context.atlas().region(occupant.spriteName());
            int x = layout.figureLeft(cell);
            // Le héros est retourné selon son orientation ; c'est la lecture la plus immédiate de
            // « où est-ce que je regarde ».
            if (occupant == arena.hero() && arena.hero().facing() == Direction.LEFT) {
                painter.spriteFlipped(region, x, ArenaLayout.FIGURE_Y);
            } else {
                painter.sprite(region, x, ArenaLayout.FIGURE_Y);
            }
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

    @Override
    public int contentTopWorldY() {
        // Le bandeau ne doit pas mordre sur la tête des figures, le contenu le plus haut.
        return ArenaLayout.FIGURE_Y + ArenaLayout.FIGURE_HEIGHT + 2;
    }

    @Override
    public List<String> overlayLines(int screenWidth, int screenHeight) {
        List<String> lines = new ArrayList<>();
        lines.add("STARFALL - JALON M4 - GRILLE ET HÉROS");
        lines.add("GRILLE : " + layout.gridWidth() + " CASES   HÉROS : CASE "
                + (arena.heroCell() + 1) + "   REGARD : " + arena.hero().facing().label().toUpperCase());

        int target = arena.swapTarget();
        lines.add("ÉCHANGE : " + (target < 0 ? "AUCUNE CIBLE" : "CASE " + (target + 1)
                + " (" + arena.grid().occupantAt(target).label().toUpperCase() + ")"));

        if (lastResult != null) {
            lines.add("DERNIÈRE ACTION : " + lastResult.label().toUpperCase());
        }
        lines.add("FLÈCHES OU A/Q ET D : SE TOURNER PUIS AVANCER   E : ÉCHANGE   CLIC : LES DEUX");
        lines.add("ÉCHAP : QUITTER   F11 : PLEIN ÉCRAN   ESPACE : CAMÉRA DE CONTRÔLE");
        return lines;
    }
}
