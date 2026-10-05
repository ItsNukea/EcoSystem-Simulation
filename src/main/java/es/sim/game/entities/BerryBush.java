package es.sim.game.entities;

import es.sim.game.*;
import es.sim.game.board.*;
import es.sim.texture.*;
import es.sim.util.*;

import java.awt.*;
import java.util.*;
import java.util.zip.*;

public class BerryBush extends Entity {
    private final int MAX_GROWING_STAGE = 2;
    private int MAX_SEED_COOLDOWN;

    private int growingStage = 0;
    private int remainingSeedCooldown = 300; //5x: same real-world value at the new, 5x higher tps
    public double growSpeed = 1f;

    protected BerryBush() {
        super(Identifier.of("entity:berrybush"));
    }

    /// Creates a new {@link BerryBush} with random properties
    public static BerryBush getNewWithRandomProperties(Rectangle positionBounds) {
        Random random = new Random();
        BerryBush created = new BerryBush();

        created.growingStage = random.nextInt(0, 3);
        created.pos = new Point(random.nextInt(positionBounds.width), random.nextInt(positionBounds.height));
        created.MAX_SEED_COOLDOWN = 100 + random.nextInt(400); //5x: same real-world range at the new, 5x higher tps
        created.remainingSeedCooldown = created.MAX_SEED_COOLDOWN;
        //Multiply the grow speed with a random value in a logarithmically distributed range between 0.5 and 2
        //This means that multiplying by a number has the same chance as dividing by that same number
        created.growSpeed *= Util.linearToLogarithmicDistribution(random.nextDouble(), 0.5, 2, 1.5);

        return created;
    }

    @Override
    public void tick() {
        age++;
        remainingSeedCooldown--;
        if(new Random().nextInt(5000) == 3) { //5x: same real-world probability at the new, 5x higher tps
            growingStage = Math.min(MAX_GROWING_STAGE, growingStage + 1);
        }

        analyzeSurroundings();
    }

    @Override
    public void render(Graphics2D graphics, Rectangle cellBounds) {
        Texture sprite = new Texture(getResourceLocation());
        int spriteWidth = sprite.getWidth();
        int spriteHeight = sprite.getHeight();

        double scale = Math.min(
                (double) cellBounds.width / spriteWidth,
                (double) cellBounds.height / spriteHeight
        );

        int drawWidth = (int) (spriteWidth * scale);
        int drawHeight = (int) (spriteHeight * scale);

        int drawX = cellBounds.x + (cellBounds.width - drawWidth) / 2;
        int drawY = cellBounds.y + (cellBounds.height - drawHeight) / 2;

        graphics.drawImage(
                sprite.asImage(),
                drawX,
                drawY,
                drawWidth,
                drawHeight,
                null
        );
    }

    @Override
    protected void analyzeSurroundings() {
        surroundings = Surroundings.ofEntity(this);
        if(remainingSeedCooldown <= 0) {
            ACTIVITY = EntityActivity.BREEDING;
            return;
        }
        ACTIVITY = EntityActivity.WANDERING;
    }

    private Identifier getResourceLocation() {
        String baseID = getEntityID().getPath();
        return new Identifier("entity", baseID + "_" + growingStage);
    }

    public boolean hasBerries() {
        return growingStage == MAX_GROWING_STAGE;
    }

    public void eatBerries() {
        if (!hasBerries()) return;

        growingStage--;
    }
}
