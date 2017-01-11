package celtech.utils.Math.Packing.core;

import celtech.utils.Math.Packing.primitives.MArea;
import java.awt.Dimension;
import java.util.ArrayList;

public class BinPacking {
	/**
	 * Entry point for the application. Applies the packing strategies to the
	 * provided pieces.
	 *
	 * @param pieces            pieces to be nested inside the bins.
	 * @param binDimension      dimensions for the generated bins.
	 * @param viewPortDimension dimensions of the view port for the bin images generation
	 * @return list of generated bins.
	 */
	public static Bin[] BinPackingStrategy(MArea[] pieces, Dimension binDimension, Dimension viewPortDimension) {
		ArrayList<Bin> bins = new ArrayList<Bin>();
		int nbin = 0;
		boolean stillToPlace = true;
		MArea[] notPlaced = pieces;
		while (stillToPlace) {
			stillToPlace = false;
			Bin bin = new Bin(binDimension);
			notPlaced = bin.BBCompleteStrategy(notPlaced);

			bin.compress();

			notPlaced = bin.dropPieces(notPlaced);

			bins.add(bin);
			if (notPlaced.length > 0)
				stillToPlace = true;
		}

		return bins.toArray(new Bin[0]);
	}

}
