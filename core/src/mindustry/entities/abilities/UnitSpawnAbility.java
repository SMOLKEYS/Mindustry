package mindustry.entities.abilities;

import arc.*;
import arc.struct.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.*;
import mindustry.io.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;

import static mindustry.Vars.*;

public class UnitSpawnAbility extends Ability{
    public UnitType unit;
    public int limit = -1;
    public float spawnTime = 60f, spawnX, spawnY;
    public Effect spawnEffect = Fx.spawn;
    public boolean parentizeEffects;

    protected float timer;
    protected Seq<Unit> units = new Seq<>();

    public UnitSpawnAbility(UnitType unit, float spawnTime, float spawnX, float spawnY){
        this.unit = unit;
        this.spawnTime = spawnTime;
        this.spawnX = spawnX;
        this.spawnY = spawnY;
    }

    public UnitSpawnAbility(){
    }

    @Override
    public void addStats(Table t){
        super.addStats(t);
        t.add(abilityStat("buildtime", Strings.autoFixed(spawnTime / 60f, 2)));
        t.row();
        t.add((unit.hasEmoji() ? unit.emoji() : "") + "[stat]" + unit.localizedName);
    }

    @Override
    public void update(Unit unit){
        timer += Time.delta * state.rules.unitBuildSpeed(unit.team);

        units.remove(u -> u.dead() || !u.isValid() || u.spawner != unit);

        if(timer >= spawnTime && Units.canCreate(unit.team, this.unit) && units.size < limit){
            float x = unit.x + Angles.trnsx(unit.rotation, spawnY, -spawnX), y = unit.y + Angles.trnsy(unit.rotation, spawnY, -spawnX);
            spawnEffect.at(x, y, 0f, parentizeEffects ? unit : null);
            Unit u = this.unit.create(unit.team);
            u.set(x, y);
            u.rotation = unit.rotation;
            Events.fire(new UnitCreateEvent(u, null, unit));
            if(!Vars.net.client()){
                u.add();
                Units.notifyUnitSpawn(u);
                spawned(u);
                units.add(u);
            }

            timer = 0f;
        }
    }

    public void spawned(Unit spawnedUnit){
        
    }

    @Override
    public void draw(Unit unit){
        if(Units.canCreate(unit.team, this.unit)){
            Draw.draw(Draw.z(), () -> {
                float x = unit.x + Angles.trnsx(unit.rotation, spawnY, -spawnX), y = unit.y + Angles.trnsy(unit.rotation, spawnY, -spawnX);
                Drawf.construct(x, y, this.unit.fullIcon, unit.rotation - 90, timer / spawnTime, 1f, timer);
            });
        }
    }

    @Override
    public void read(Reads read){
        super.read(read);

        for(int i = 0; i < read.i(); i++){
            units.add(TypeIO.readUnit(read));
        }
    }

    @Override
    public void write(Writes write){
        super.write(write);
        
        write.i(units.size);
        units.each(u -> TypeIO.writeUnit(write, u));
    }

    @Override
    public String localized(){
        return Core.bundle.format("ability.unitspawn", unit.localizedName);
    }
}
