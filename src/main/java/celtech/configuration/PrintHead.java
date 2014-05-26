/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.configuration;

/**
 *
 * @author Ian Hudson @ Liberty Systems Limited
 */
public enum PrintHead
{

    /**
     *
     */
    UNRECOGNISED("Unrecognised type", "Unrecognised"),

    /**
     *
     */
    RBX01_SM("Dual nozzle single melt chamber head with 0.3/0.8mm nozzles", "Dual 0.3/0.8mm");

    private String friendlyHeadName;
    private String shortHeadName;

    private PrintHead(String friendlyHeadName, String shortHeadName)
    {
        this.friendlyHeadName = friendlyHeadName;
        this.shortHeadName = shortHeadName;
    }

    /**
     *
     * @return
     */
    public String getFriendlyHeadName()
    {
        return friendlyHeadName;
    }

    /**
     *
     * @return
     */
    public String getShortName()
    {
        return shortHeadName;
    }

    /**
     *
     * @param headTypeCode
     * @return
     */
    public static PrintHead getPrintHeadForType(final String headTypeCode)
    {
        PrintHead printHead = null;
        //interchangeably looks for RBX-ABS-GR499 or RBX_ABS_GR499
        String headTypeCodeToCheck = headTypeCode.trim();
        try
        {
            printHead = PrintHead.valueOf(headTypeCodeToCheck);
        } catch (IllegalArgumentException ex)
        {
            try
            {
                printHead = PrintHead.valueOf(headTypeCodeToCheck.replaceAll("-", "_"));
            } catch (IllegalArgumentException ex1)
            {
                printHead = PrintHead.UNRECOGNISED;
            }
        }

        return printHead;
    }
}
