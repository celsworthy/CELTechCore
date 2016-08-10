package celtech.configuration;

/**
 *
 * @author Ian Hudson @ Liberty Systems Limited
 */
public enum PrinterEdition
{

    Kickstarter("KS", "Kickstarter Edition"),
    TradeSample(
            "TS", "Trade Sample"),
    SingleHeadUK(
            "BK", "Single head only UK"),
    SingleHeadEurope(
            "BE", "Single head only Europe"),
    SingleHeadUSA(
            "BU", "Single head only USA"),
    SingleHeadSouthAsia_Australia(
            "BA", "Single head only South Asia / Australia"),
    SingleHeadJapan(
            "BN", "Single head only Japan"),
    SingleHeadChina(
            "BC", "Single head only China"),
    SingleHeadSwitzerland(
            "BV", "Single head only Switzerland"),
    SingleHeadWorldwide(
            "BW", "Single head only Worldwide"),
    SingleHeadKorea(
            "BR", "Single head only Korea"),
    SingleHeadIsrael(
            "BI", "Single head only Israel"),
    TwinHeadUK(
            "SK", "Twin head UK"),
    TwinHeadEurope(
            "SE", "Twin head Europe"),
    TwinHeadUSA(
            "SU", "Twin head USA"),
    TwinHeadSouthAsia_Australia(
            "SA", "Twin head South Asia / Australia"),
    TwinHeadJapan(
            "SN", "Twin head Japan"),
    TwinHeadChina(
            "SC", "Twin head China"),
    TwinHeadSwitzerland(
            "SV", "Twin head Switzerland"),
    TwinHeadWorldwide(
            "SW", "Twin head Worldwide"),
    TwinHeadKorea(
            "SR", "Twin head Korea"),
    TwinHeadIsrael(
            "SI", "Twin head Israel");

    private final String codeName;
    private final String friendlyName;

    private PrinterEdition(String codeName, String friendlyName)
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
    @Override
    public String toString()
    {
        return getCodeName() + " - " + getFriendlyName();
    }
}
