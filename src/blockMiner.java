import org.rspeer.runetek.adapter.component.Item;
import org.rspeer.runetek.adapter.scene.SceneObject;
import org.rspeer.runetek.api.commons.BankLocation;
import org.rspeer.runetek.api.commons.StopWatch;
import org.rspeer.runetek.api.commons.Time;
import org.rspeer.runetek.api.commons.math.Random;
import org.rspeer.runetek.api.component.Bank;
import org.rspeer.runetek.api.component.tab.Equipment;
import org.rspeer.runetek.api.component.tab.EquipmentSlot;
import org.rspeer.runetek.api.component.tab.Inventory;
import org.rspeer.runetek.api.component.tab.Skill;
import org.rspeer.runetek.api.movement.Movement;
import org.rspeer.runetek.api.movement.position.Area;
import org.rspeer.runetek.api.scene.Players;
import org.rspeer.runetek.api.scene.SceneObjects;
import org.rspeer.runetek.event.listeners.RenderListener;
import org.rspeer.runetek.event.listeners.SkillListener;
import org.rspeer.runetek.event.types.RenderEvent;
import org.rspeer.runetek.event.types.SkillEvent;
import org.rspeer.script.Script;
import org.rspeer.script.ScriptCategory;
import org.rspeer.script.ScriptMeta;
import org.rspeer.ui.Log;

import java.awt.*;

import static org.rspeer.runetek.api.component.tab.Equipment.getSlot;


@ScriptMeta(name = "Block Miner",  desc = "Script description", developer = "Developer's Name", category = ScriptCategory.MONEY_MAKING)
public class blockMiner extends Script implements SkillListener, RenderListener {
    public static String bankChest = "Bank chest";
    public static String runeStone = "Dense runestone";
    private int blocksMined = 0;
    private static Area bankArea = Area.rectangular(1638, 3945, 1640, 3943);
    private static Area darkAltar = Area.rectangular(1712, 3887, 1721, 3879);
    private static Area essMine = Area.rectangular(1760, 3857, 1767, 3848);
    private String GAMES_NECK = "Games necklace(8)";
    private StopWatch timer;

    @Override
    public void onStart()
    {
        timer = StopWatch.start();
    }
    @Override
    public int loop() {
        if(Inventory.isFull() && Inventory.contains("Dark essence block")) // deposit blocks
        {
            if(Bank.isClosed())
            {
                SceneObject bankchest = SceneObjects.getNearest(bankChest);
                if(bankchest == null)
                {
                    EquipmentSlot amuletSlot = getSlot(a-> a.getName().contains("Games")); // use games neck
                    if(amuletSlot != null)
                    {
                        amuletSlot.interact("Wintertodt Camp");
                        Time.sleep(800);
                    }
                    else
                        Log.info("Cannot find games neck");
                }
                else if(bankchest != null)
                {
                    bankchest.interact("Bank");
                    toggleRun();
                    Time.sleepUntil(()-> Bank.isOpen(), Random.low(3333,5555));
                }
                Time.sleepUntil(Bank::isOpen ,Random.low(3333,6666));
            }
            else
            {
                Bank.depositAll("Dark essence block");
                Time.sleepUntil(()-> !Inventory.contains("Dark essence block"), Random.low(1111,2222));
            }

        }
        else if(Inventory.isFull() && Inventory.contains("Dense essence block")) // goto use blocks on altar
        {
            if(!darkAltar.contains(Players.getLocal()))
            {
                Movement.walkToRandomized(darkAltar.getCenter());
                toggleRun();
                Time.sleepUntil(()-> !Players.getLocal().isMoving() || darkAltar.contains(Players.getLocal()), Random.low( 1333, 2222));
            }
            else
            {
                SceneObjects.getNearest("Dark altar").interact("Venerate");
                Time.sleepUntil(()-> !Inventory.contains("Dense essence block"), Random.low(222,5555));
                blocksMined--;
            }
        }
        else if(!Inventory.isFull() && !Equipment.isOccupied(EquipmentSlot.NECK)) // withdraw games necks
        {
            if(Bank.isOpen() && Inventory.getCount(GAMES_NECK) == 0)
            {
                if (Bank.getCount(GAMES_NECK) > 0) {
                    Bank.withdraw(GAMES_NECK, 1);
                    Time.sleepUntil(() -> Inventory.getCount(GAMES_NECK) > 0, Random.low(888, 1222));
                }
            }
            else if(Inventory.getCount(GAMES_NECK) != 0)
            {
                Inventory.getFirst(GAMES_NECK).interact("Wear");
                Time.sleepUntil(()-> Equipment.isOccupied(EquipmentSlot.NECK),2222);
            }
            else
            {
                SceneObjects.getNearest(bankChest).interact("Bank");
                Time.sleepUntil(()-> Bank.isOpen(), 2222);
            }

        }
        else if(!Inventory.isFull() && Movement.getRunEnergy() < 60 && bankArea.contains(Players.getLocal())) // eat strange fruit till high run energy
        {
            if(Inventory.contains("Strange fruit"))
            {
                Inventory.getFirst("Strange fruit").interact("Eat");
                Time.sleepUntil(()-> Inventory.getCount("Strange fruit") == 0, 1000);
            }
            else if(Bank.isOpen())
            {
                Bank.withdraw("Strange fruit", 1);
                Time.sleepUntil(()-> Inventory.getCount("Strange fruit") > 0, 3000);
            }
            else
            {
                SceneObjects.getNearest(bankChest).interact("Bank");
                Time.sleepUntil(()-> Bank.isOpen(), 3000);
            }
        }
        else if(!Inventory.isFull() && bankArea.contains(Players.getLocal()) && !Movement.isStaminaEnhancementActive()) // drink a stamina if not active
        {
            if(haveStaminas())
            {
                Inventory.getFirst(a-> a.getName().contains("Stamina")).interact("Drink");
                Time.sleepUntil(()-> Movement.isStaminaEnhancementActive(), 2222);
            }
            else if(Bank.isOpen())
            {
                Bank.getFirst(a-> a.getName().contains("Stamina")).interact("Withdraw-1");
                Time.sleepUntil(()-> haveStaminas(), 3333);
            }
            else
            {
                SceneObjects.getNearest(bankChest).interact("Bank");
                Time.sleepUntil(()-> Bank.isOpen(), 3333);
            }

        }
        else if(!Inventory.isFull() && bankArea.contains(Players.getLocal()) && Movement.isStaminaEnhancementActive() && haveStaminas()) // deposit stams we dont need
        {
            if(Bank.isOpen())
            {
                Bank.deposit(a-> a.getName().contains("Stamina"),1);
                Time.sleepUntil(()-> !haveStaminas(), 3000);
            }
            else
            {
                SceneObjects.getNearest(bankChest).interact("Bank");
                Time.sleepUntil(()-> Bank.isOpen(), 3000);
            }
        }
        else if(!Inventory.isFull() && essMine.contains(Players.getLocal())) // mine runestone
        {
            if(!Players.getLocal().isAnimating())
            {
                SceneObjects.getNearest(runeStone).interact("Chip");
                toggleRun();
                Time.sleepUntil(()-> Players.getLocal().isAnimating(), 3000);
            }
        }
        else if(!Inventory.isFull() && !essMine.contains(Players.getLocal())) // walk to runestone
        {
            if(Inventory.getCount("Vial") > 0)
            {
                Inventory.getFirst("Vial").interact("Drop");
            }
            Movement.walkToRandomized(essMine.getCenter());
            toggleRun();
            Time.sleepUntil(()-> !Players.getLocal().isMoving() || essMine.contains(Players.getLocal()), Random.low( 1221, 3323));
        }
        return Random.low(333,999);
    }

    @Override
    public void notify(RenderEvent renderEvent) {
        Graphics g = renderEvent.getSource();
        g.setColor(new Color(0,0,0,150));
        g.fillRoundRect(5,30,150,130,10,10);
        g.setFont(new Font("Arial", Font.BOLD, 14));
        g.setColor(Color.white);
        g.drawString("Runtime: " + timer.toElapsedString(), 10,50);
        g.drawString("Blocks Mined: " + blocksMined/2, 10,70);
        g.drawString("Blocks/hr: " + Math.floor(timer.getHourlyRate(blocksMined/2)), 10,90);

    }
    @Override
    public void notify(SkillEvent skillEvent) {
        if(skillEvent.getType() == SkillEvent.TYPE_EXPERIENCE)
        {
            blocksMined++;
        }

    }
    private boolean toggleRun()
    {
        if (!Movement.isRunEnabled() && Movement.getRunEnergy() > Random.nextInt(10, 30)) { // If our energy is higher than a random value 10-30
            Movement.toggleRun(true); // Toggle run
            return true;
        }
        return false;
    }
    private boolean haveStaminas()
    {
        if(Inventory.getFirst(a-> a.getName().contains("Stamina")) != null)
        {
            return true;
        }
        else
            return false;
    }

}
