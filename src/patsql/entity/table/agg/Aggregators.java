package patsql.entity.table.agg;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;

import patsql.entity.table.AggColSchema;
import patsql.entity.table.ColSchema;
import patsql.entity.table.Table;

public class Aggregators {
	//AggColSchema includes agg name and corresponding cols eg:max(col1)
	public final AggColSchema[] aggColSchemas;

	public Aggregators(AggColSchema... aggColSchemas) {
		this.aggColSchemas = aggColSchemas;
	}

	//enumerate all valid aggregate and column combinations
	public static Aggregators all(ColSchema... srcCols) {
		List<AggColSchema> ret = new ArrayList<>();

		for (Agg agg : Agg.values()) {
			for (ColSchema src : srcCols) {
				if (!agg.isComputable(src.type))
					continue;

				// avoid MAX(ConcatComma(...))
				if (src instanceof AggColSchema) {
					AggColSchema ag = (AggColSchema) src;
					if (ag.agg == Agg.ConcatComma || ag.agg == Agg.ConcatSlash || ag.agg == Agg.ConcatSpace)
						continue;
				}
				ret.add(new AggColSchema(agg, src));
			}
		}
		return new Aggregators(ret.toArray(new AggColSchema[0]));
	}

	//infer agg from output table
	//skip enumeration, directly infer the agg name via output col name
	//method 1: get all the aggs we can infer from output table col name
	//enumerate the combinations of all the aggs we inferred and all the col names 
	public static Aggregators allByInferringAggregators(Table output, ColSchema... srcCols) {
		List<AggColSchema> ret = new ArrayList<>();

		Set<Agg> aggSet = new HashSet<>();
		for(ColSchema outputCol:output.schema()){
			if(outputCol.aggs.size()!=0){
				aggSet.addAll(outputCol.aggs);
			}
		}

		//if there is no agg in the col name, we have to list all the combinations of aggs and cols
		// if(aggSet.size()==0){
		// 	return all(srcCols);
		// }
		//if there is no agg in the col name, we return nothing
		if(aggSet.size()==0){
			return new Aggregators(ret.toArray(new AggColSchema[0]));
		}

		for (Agg agg : aggSet) {
			for (ColSchema src : srcCols) {
				if (!agg.isComputable(src.type))
					continue;

				// avoid MAX(ConcatComma(...))
				if (src instanceof AggColSchema) {
					AggColSchema ag = (AggColSchema) src;
					if (ag.agg == Agg.ConcatComma || ag.agg == Agg.ConcatSlash || ag.agg == Agg.ConcatSpace)
						continue;
				}
				ret.add(new AggColSchema(agg, src));
			}
		}
		return new Aggregators(ret.toArray(new AggColSchema[0]));
	}

	//further narrow down the search space, if the info source of one output column it's the same as the info source of one srcCols
	//and this output column is computed by aggr function, then add this combination to the result
	public static Aggregators allByInferringAggsandColNames(Table output, Table tmpTable) {
		Set<AggColSchema> ret = new HashSet<>();
		Map<String, List<ColSchema>> srcColName2ColSchema = tmpTable.getSrcColName2ColSchemaMap();

		for(ColSchema outputCol:output.schema()){
			if(outputCol.aggs.size()==0) continue;

			// if the outputCol name contains aggregation, enumerate all the possible combinations of agg and srcCol that can generate that outputCol
			// enumerate all the aggs in the outputCol name
			for(Agg agg:outputCol.aggs){
				// if we can find some srcCols whose srcColName is the same as the srcColName of the outputCol, add this combination to the result
				if(srcColName2ColSchema.containsKey(outputCol.getSrcColName())){
					for(ColSchema colSchema:srcColName2ColSchema.get(outputCol.getSrcColName())){
						ret.add(new AggColSchema(agg, colSchema));
					}
				}
				// if we cannot find, we need to add all the combination of this agg to the result
				else{
					for(ColSchema src:tmpTable.schema()){
						ret.add(new AggColSchema(agg, src));
					}
				}
				
			}
		}
		return new Aggregators(ret.toArray(new AggColSchema[0]));
	}

	public static Aggregators empty() {
		return new Aggregators();
	}

	@Override
	public String toString() {
		return Arrays.toString(aggColSchemas);
	}

}
