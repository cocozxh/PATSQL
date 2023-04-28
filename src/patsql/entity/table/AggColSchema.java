package patsql.entity.table;

import patsql.entity.table.agg.Agg;

public class AggColSchema extends ColSchema {
	public final Agg agg;
	public final ColSchema src;

	public AggColSchema(Agg agg, ColSchema src) {
		// super(agg + "(" + src.name + ")", agg.returnType(src.type));
		super(agg.toString().toLowerCase()+src.name.substring(0, 1).toUpperCase()+src.name.substring(1), agg.returnType(src.type));
		this.setSrcColName(src.name);
		this.agg = agg;
		this.src = src;
	}

	public int srcColId() {
		return src.id;
	}

	@Override
	public boolean equals(Object obj){
		if(obj == this) return true;
		if(obj == null || obj.getClass() != this.getClass()) return false;
		AggColSchema otherColSchema = (AggColSchema) obj;
		return this.src.equals(otherColSchema.src) && this.agg == otherColSchema.agg;
	}

	@Override
	public int hashCode(){
		return agg.hashCode()+src.hashCode();
	}
}
