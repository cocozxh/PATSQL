package patsql.synth.filler.strategy;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import patsql.entity.synth.Example;
import patsql.entity.synth.SynthOption;
import patsql.entity.table.ColSchema;
import patsql.entity.table.Column;
import patsql.entity.table.Table;
import patsql.ra.operator.Projection;
import patsql.ra.operator.RA;
import patsql.ra.operator.RAOperator;
import patsql.ra.util.Utils;
import patsql.synth.filler.ColRelation;

/**
 * In this algorithm, a projection sketch is filled by matching the intermediate
 * columns to the output columns. The matching criteria is given as "relation".
 */
public class ProjectionMatching implements FillingStrategy {
	final ColRelation relation;

	public ProjectionMatching(ColRelation relation) {
		this.relation = relation;
	}

	@Override
	public RA targetKind() {
		return RA.PROJECTION;
	}

	@Override
	public List<RAOperator> fill(RAOperator sketch, Example example, SynthOption option) {
		if (sketch.kind != targetKind()) {
			throw new IllegalStateException("Invalid filling strategy.");
		}
		Projection target = (Projection) sketch;
		Table tmpTable = target.child.eval(example.tableEnv());
		Table outTable = example.output;
	
		// match columns between the intermediate and output tables.
		List<List<Column>> pairsForEachOutCol = getAllPairs(tmpTable, outTable);
		// List<List<Column>> pairsForEachOutCol = inferPairsBySrcColName(tmpTable, outTable);


		// fill sketches.
		List<RAOperator> ret = new ArrayList<>();
		for (List<Column> cols : Utils.cartesianProduct(pairsForEachOutCol)) {
			Projection clone = target.clone();
			clone.projCols = toCols(cols);
			ret.add(clone);
		}
		return ret;
	}

	private List<List<Column>> getAllPairs(Table tmpTable, Table outTable){
		List<List<Column>> pairsForEachOutCol = new ArrayList<>();
		for (Column outCol : outTable.columns) {
			List<Column> pairs = new ArrayList<>();
			for (Column tmpCol : tmpTable.columns) {
				if (relation.compare(outCol, tmpCol)) {
					if (tmpCol.hasSameName(outCol)) {
						// if there exists a column with the same name, use only it.
						pairs.clear();
						pairs.add(tmpCol);
						break;
					} else {
						pairs.add(tmpCol);
					}
				}
			}
			pairsForEachOutCol.add(pairs);
		}
		return pairsForEachOutCol;
	}

	private List<List<Column>> inferPairsBySrcColName(Table tmpTable, Table outTable){
		Map<String, List<Column>> srcColName2Col = tmpTable.getSrcColName2ColMap();
			
		// match columns between the intermediate and output tables.
		List<List<Column>> pairsForEachOutCol = new ArrayList<>();

		for (Column outCol : outTable.columns) {
			String outColSrcCol = outCol.schema.getSrcColName();
			boolean found = false;
			List<Column> pairs = new ArrayList<>();
			List<Column> tmpCols = srcColName2Col.getOrDefault(outColSrcCol, new ArrayList<>());
			for (Column tmpCol : tmpCols) {
				if (relation.compare(outCol, tmpCol)) {
					found = true;
					if (tmpCol.hasSameName(outCol)) {
						// if there exists a column with the same name, use only it.
						pairs.clear();
						pairs.add(tmpCol);
						break;
					} else {
						pairs.add(tmpCol);
					}
				}
			}
			//if did not find any column has the same srcColName as the outCol
			//or it did find some, but relation compare never succeed
			//we need to check all columns in tmpTable
			if(!found){
				for (Column tmpCol : tmpTable.columns) {
					if (relation.compare(outCol, tmpCol)) {
						found = true;
						if (tmpCol.hasSameName(outCol)) {
							// if there exists a column with the same name, use only it.
							pairs.clear();
							pairs.add(tmpCol);
							break;
						} else {
							pairs.add(tmpCol);
						}
					}
				}
			}
			pairsForEachOutCol.add(pairs);
		}
		return pairsForEachOutCol;
	}

	private ColSchema[] toCols(List<Column> cols) {
		ColSchema[] ret = new ColSchema[cols.size()];
		for (int i = 0; i < ret.length; i++) {
			ret[i] = cols.get(i).schema;
		}
		return ret;
	}

}
