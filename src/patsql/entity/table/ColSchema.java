package patsql.entity.table;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

import patsql.entity.table.agg.Agg;
import patsql.entity.table.window.WinFunc;

public class ColSchema {
	public final Type type;
	public final String name;
	public final int id;
	public final String[] splits;
	public final Set<Agg> aggs;
	public final Set<WinFunc> winFuncs;
	private String srcColName;
	private static int colId = 0;

	public ColSchema(String name, Type type) {
		this.type = type;
		this.name = name;
		this.id = colId;
		this.splits = splitName();
		this.aggs = getAllAggs();
		this.winFuncs = getAllWinFuncs();
		this.srcColName = this.splits[this.splits.length - 1].toLowerCase();
		// TODO: if this does not work, we can get source column name step by step from
		// the input column name
		// ex: input column name: age, after select operation, the sourceColName of the
		// result column name is still equal to input column name
		colId++;
	}

	public ColSchema(String name, Type type, int id) {
		this.type = type;
		this.name = name;
		this.id = id;
		this.splits = splitName();
		this.aggs = getAllAggs();
		this.winFuncs = getAllWinFuncs();
		this.srcColName = this.splits[this.splits.length - 1].toLowerCase();
	}

	public static int newID() {
		int ret = colId;
		colId++;
		return ret;
	}

	@Override
	public String toString() {
		return "[" + id + "] " + name + ":" + type;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this)
			return true;
		if (obj == null || obj.getClass() != this.getClass())
			return false;
		ColSchema otherColSchema = (ColSchema) obj;
		return this.id == otherColSchema.id && this.type == otherColSchema.type
				&& this.name.equals(otherColSchema.name);
	}

	@Override
	public int hashCode() {
		return this.id + this.type.hashCode() + this.name.hashCode();
	}

	public void setSrcColName(String srcColName) {
		this.srcColName = srcColName;
	}

	public String getSrcColName(){
		return this.srcColName;
	}

	private String[] splitName() {
		String[] temp = name.split("(?<!^)(?=[A-Z])");
		List<String> res = new ArrayList<>();
		for (int i = 0; i < temp.length; i++) {
			if (temp[i].toLowerCase().equals("concat")) {
				res.add(temp[i] + temp[i + 1]);
				i++;
			} else {
				res.add(temp[i]);
			}
		}
		return res.toArray(new String[0]);
	}

	private Set<Agg> getAllAggs() {
		Set<Agg> result = new HashSet<>();
		for (String n : this.splits) {
			Agg temp = Agg.findAggByName(n);
			if (temp != null)
				result.add(temp);
		}
		return result;
	}

	private Set<WinFunc> getAllWinFuncs() {
		Set<WinFunc> result = new HashSet<>();
		for (String n : this.splits) {
			WinFunc temp = WinFunc.findWinFuncByName(n);
			if (temp != null)
				result.add(temp);
		}
		return result;
	}

	private Set<String> possiblePartitionKeys() {
		Set<String> result = new HashSet<>();
		for (String n : this.splits) {
			result.add(n);
			WinFunc temp = WinFunc.findWinFuncByName(n);
			if (temp != null) {
				break;
			}
		}
		return result;
	}
}
