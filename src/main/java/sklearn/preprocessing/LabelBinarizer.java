/*
 * Copyright (c) 2015 Villu Ruusmann
 *
 * This file is part of JPMML-SkLearn
 *
 * JPMML-SkLearn is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * JPMML-SkLearn is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with JPMML-SkLearn.  If not, see <http://www.gnu.org/licenses/>.
 */
package sklearn.preprocessing;

import java.util.ArrayList;
import java.util.List;

import org.dmg.pmml.Apply;
import org.dmg.pmml.DataType;
import org.dmg.pmml.DerivedField;
import org.dmg.pmml.OpType;
import org.jpmml.converter.BinaryFeature;
import org.jpmml.converter.ContinuousFeature;
import org.jpmml.converter.Feature;
import org.jpmml.converter.PMMLUtil;
import org.jpmml.converter.ValueUtil;
import org.jpmml.sklearn.ClassDictUtil;
import org.jpmml.sklearn.SkLearnEncoder;
import sklearn.Transformer;
import sklearn.TypeUtil;

public class LabelBinarizer extends Transformer {

	public LabelBinarizer(String module, String name){
		super(module, name);
	}

	@Override
	public OpType getOpType(){
		return OpType.CATEGORICAL;
	}

	@Override
	public DataType getDataType(){
		List<?> classes = getClasses();

		return TypeUtil.getDataType(classes, DataType.STRING);
	}

	@Override
	public List<Feature> encodeFeatures(List<String> ids, List<Feature> features, SkLearnEncoder encoder){
		List<?> classes = getClasses();

		Number posLabel = getPosLabel();
		Number negLabel = getNegLabel();

		ClassDictUtil.checkSize(1, ids, features);

		String id = ids.get(0);
		Feature feature = features.get(0);

		List<String> categories = new ArrayList<>();

		ids.clear();

		List<Feature> result = new ArrayList<>();

		for(int i = 0; i < classes.size(); i++){
			Object value = classes.get(i);

			ids.add(id + "=" + ValueUtil.formatValue(value));

			if(ValueUtil.isOne(posLabel) && ValueUtil.isZero(negLabel)){
				String category = ValueUtil.formatValue(value);

				categories.add(category);

				result.add(new BinaryFeature(encoder, feature.getName(), DataType.STRING, category));
			} else

			{
				// "($name == value) ? pos_label : neg_label"
				Apply apply = PMMLUtil.createApply("if", PMMLUtil.createApply("equal", feature.ref(), PMMLUtil.createConstant(value)), PMMLUtil.createConstant(posLabel), PMMLUtil.createConstant(negLabel));

				DerivedField derivedField = encoder.createDerivedField(createName(id, i), apply);

				result.add(new ContinuousFeature(encoder, derivedField));
			}
		}

		encoder.updateValueSpace(feature.getName(), categories);

		return result;
	}

	public List<?> getClasses(){
		return (List)ClassDictUtil.getArray(this, "classes_");
	}

	public Number getPosLabel(){
		return (Number)get("pos_label");
	}

	public Number getNegLabel(){
		return (Number)get("neg_label");
	}
}