package celtech.configuration;

/**
 *
 * @author Ian Hudson @ Liberty Systems Limited
 */
public enum PrinterModel
{

    /**
     *
     */
    Robox("RBX01", "Robox version 1"),
    Robox_2("RBX02", "Robox version 2");

    private final String codeName;
    private final String friendlyName;

    private PrinterModel(String codeName, String friendlyName)
    {
        this.codeName = codeName;
        this.friendlyName = friendlyName;
    }

    /**
     *
     * @return
     */
    public String getCodeName()
    {
        return codeName;
    }

    /**
     *
     * @return
     */
    public String getFriendlyName()
    {
        return friendlyName;
    }

//    public static PrinterModel getPrintHeadForType(final String headTypeCode)
//    {
//        PrinterModel printHead = null;
//        //interchangeably looks for RBX-ABS-GR499 or RBX_ABS_GR499
//        String headTypeCodeToCheck = headTypeCode.trim();
//        try
//        {
//            printHead = PrinterModel.valueOf(headTypeCodeToCheck);
//        } catch (IllegalArgumentException ex)
//        {
//            try
//            {
//                printHead = PrinterModel.valueOf(headTypeCodeToCheck.replaceAll("-", "_"));
//            } catch (IllegalArgumentException ex1)
//            {
//                printHead = PrinterModel.UNRECOGNISED;
//            }
//        }
//
//        return printHead;
//    }
    
    /**
     *
     * @return
     */
        
    @Override
    public String toString()
    {
        return getFriendlyName();
    }
}
