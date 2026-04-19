package ui;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import global.ApplicationUtils;

import java.io.File;

public class Resources
{
    public static FlatSVGIcon getIcon(String name)
    {
        return new FlatSVGIcon(new File(ApplicationUtils.jarFilePath + "res/icons/" + name + ".svg"))
                .derive(15, 15);
    }

    public static FlatSVGIcon getIcon(String name, int size)
    {
        return new FlatSVGIcon(new File(ApplicationUtils.jarFilePath + "res/icons/" + name + ".svg"))
                .derive(size, size);
    }
}
