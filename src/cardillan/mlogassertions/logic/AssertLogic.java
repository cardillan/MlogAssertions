package cardillan.mlogassertions.logic;

import arc.graphics.Color;
import cardillan.mlogassertions.Constants;
import mindustry.gen.Icon;
import mindustry.logic.LCategory;

public class AssertLogic {
    public static LCategory assertsCategory;

    public static void init(){
        assertsCategory = new LCategory(Constants.assertionsCategory, Color.slate, Icon.warningSmall);

        LogicStatements.register();
    }
}
