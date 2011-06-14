package org.noisemap.plugin;

/***********************************
 * ANR EvalPDU
 * IFSTTAR 11_05_2011
 * @author Nicolas FORTIN, Judicaël PICAUT
 ***********************************/

import org.noisemap.core.BR_EvalSourceV1;
import org.noisemap.core.BR_EvalSourceV2;
import org.noisemap.core.BR_EvalSourceV3;
import org.noisemap.core.BR_EvalSourceV4;
import org.noisemap.core.BR_SpectrumRepartition;
import org.noisemap.core.BR_TriGrid;
import org.noisemap.core.BTW_EvalSourceV1;
import org.noisemap.core.BTW_SpectrumRepartition;
import org.noisemap.core.Log10;
import org.noisemap.core.ST_SetNearestGeometryId;
import org.noisemap.core.ST_SetNearestZ;
import org.noisemap.core.ST_SplitLineInPoints;
import org.noisemap.core.ST_SplitSegment;
import org.noisemap.core.ST_TriangleContouring;
import org.gdms.sql.function.math.Power;
import org.orbisgis.core.ui.pluginSystem.Extension;
import org.orbisgis.core.ui.pluginSystem.PlugInContext;

public class NoiseMapExtension extends Extension {

	public void initialize(PlugInContext context) throws Exception {
		context.getFeatureInstaller()
				.addRegisterFunction(BR_EvalSourceV1.class);
		context.getFeatureInstaller()
				.addRegisterFunction(BR_EvalSourceV2.class);
		context.getFeatureInstaller()
				.addRegisterFunction(BR_EvalSourceV3.class);
		context.getFeatureInstaller()
				.addRegisterFunction(BR_EvalSourceV4.class);
		context.getFeatureInstaller().addRegisterFunction(
				ST_SplitLineInPoints.class);
		context.getFeatureInstaller().addRegisterFunction(Log10.class);
		context.getFeatureInstaller().addRegisterFunction(Power.class);
		context.getFeatureInstaller().addRegisterCustomQuery(
				ST_SetNearestZ.class);
		context.getFeatureInstaller().addRegisterCustomQuery(
				ST_SetNearestGeometryId.class);
		context.getFeatureInstaller()
				.addRegisterFunction(ST_SplitSegment.class);
		context.getFeatureInstaller().addRegisterCustomQuery(BR_TriGrid.class);
		context.getFeatureInstaller().addRegisterCustomQuery(
				ST_TriangleContouring.class);
		context.getFeatureInstaller().addRegisterFunction(
				BR_SpectrumRepartition.class);
		context.getFeatureInstaller().addRegisterFunction(
				BTW_SpectrumRepartition.class);
		context.getFeatureInstaller().addRegisterFunction(
				BTW_EvalSourceV1.class);
		System.out.println("LcpcSonExtension plugin loaded..");
	}

	@Override
	public void configure(PlugInContext context) throws Exception {
		this.initialize(context);
	}

	public boolean execute(PlugInContext context) throws Exception {
		/* Place you code to execute here */
		return true;
	};

	public boolean isEnabled() {
		/* Place your condition here */
		return true;
	}
}
