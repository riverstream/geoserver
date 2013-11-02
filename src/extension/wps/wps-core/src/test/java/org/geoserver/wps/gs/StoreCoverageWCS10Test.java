/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.gs;

import java.io.File;
import javax.xml.namespace.QName;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.wps.WPSTestSupport;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.gce.geotiff.GeoTiffFormat;
import org.junit.Assert;
import org.junit.Test;
import org.opengis.coverage.grid.GridCoverage;
import com.mockrunner.mock.web.MockHttpServletResponse;

/**
 * Tests WCS 1.0 support for WPS.
 */
public class StoreCoverageWCS10Test extends WPSTestSupport {

    private static final QName CUST_WATTEMP = 
            new QName(MockData.DEFAULT_URI, "watertemp", MockData.DEFAULT_PREFIX);
    static final double EPS = 1e-6;


    @Override
    protected void onSetUp(SystemTestData tData) throws Exception {
        super.onSetUp(tData);
        
        tData.addRasterLayer(CUST_WATTEMP, "custwatertemp.zip", null, null, SystemTestData.class, getCatalog());
    }
    
    @Test
    public void testStore() throws Exception {
        final String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
            "<wps:Execute service=\"WPS\" version=\"1.0.0\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" xmlns:ows=\"http://www.opengis.net/ows/1.1\" xmlns:wps=\"http://www.opengis.net/wps/1.0.0\">" +
            "<ows:Identifier>gs:StoreCoverage</ows:Identifier>" +
            "<wps:DataInputs>" +
            "  <wps:Input>" +
            "    <ows:Identifier>coverage</ows:Identifier>" +
            "    <wps:Reference xlink:href=\"http://geoserver/wcs\" method=\"POST\" mimeType=\"image/grib\">" +
            "      <wps:Body>" +
            "        <wcs:GetCoverage service=\"WCS\" version=\"1.0.0\" xmlns:wcs=\"http://www.opengis.net/wcs\" xmlns:gml=\"http://www.opengis.net/gml\">" +
            "          <wcs:sourceCoverage>" + getLayerId(CUST_WATTEMP) + "</wcs:sourceCoverage>" +
            "          <wcs:domainSubset>" +
            "            <wcs:spatialSubset>" +
            "              <gml:Envelope srsName=\"EPSG:4326\">" +
            "                <gml:pos>0.0 -91.0</gml:pos>" +
            "                <gml:pos>360.0 90.0</gml:pos>" +
            "              </gml:Envelope>" +
            "              <gml:Grid dimension=\"2\">" +
            "                <gml:limits>" +
            "                  <gml:GridEnvelope>" +
            "                    <gml:low>0 0</gml:low>" +
            "                    <gml:high>360 181</gml:high>" +
            "                  </gml:GridEnvelope>" +
            "                </gml:limits>" +
            "                <gml:axisName>x</gml:axisName>" +
            "                <gml:axisName>y</gml:axisName>" +
            "              </gml:Grid>" +
            "            </wcs:spatialSubset>" +
            "          </wcs:domainSubset>" +
            "          <wcs:output>" +
            "            <wcs:crs>EPSG:4326</wcs:crs>" +
            "            <wcs:format>GEOTIFF</wcs:format>" +
            "          </wcs:output>" +
            "        </wcs:GetCoverage>" +
            "      </wps:Body>" +
            "    </wps:Reference>" +
            "  </wps:Input>" +
            "</wps:DataInputs>" +
            "<wps:ResponseForm>" +
            "  <wps:RawDataOutput>" +
            "    <ows:Identifier>result</ows:Identifier>" +
            "  </wps:RawDataOutput>" +
            "</wps:ResponseForm>" +
            "</wps:Execute>";
        
        MockHttpServletResponse response = postAsServletResponse(root(), xml);
        String url = response.getOutputStreamContent();

        // System.out.println(url);
        Assert.assertTrue(url.startsWith("http://localhost:8080/geoserver/temp/wps/NCOM_wattemp"));
        String fileName = url.substring(url.lastIndexOf('/') + 1);

        File wpsTemp = new File(getDataDirectory().root(), "temp/wps");
        File tiffFile = new File(wpsTemp, fileName);

        Assert.assertTrue(tiffFile.exists());

        // read and check
        GeoTiffFormat format = new GeoTiffFormat();
        GridCoverage2D gc = format.getReader(tiffFile).read(null);
        scheduleForDisposal(gc);
        GridCoverage original = getCatalog().getCoverageByName(getLayerId(CUST_WATTEMP))
                .getGridCoverage(null, null);
        scheduleForDisposal(original);
        
        //
        // check the envelope did not change
        Assert.assertEquals(original.getEnvelope().getMinimum(0), gc.getEnvelope().getMinimum(0), EPS);
        Assert.assertEquals(original.getEnvelope().getMinimum(1), gc.getEnvelope().getMinimum(1), EPS);
        Assert.assertEquals(original.getEnvelope().getMaximum(0), gc.getEnvelope().getMaximum(0), EPS);
        Assert.assertEquals(original.getEnvelope().getMaximum(1), gc.getEnvelope().getMaximum(1), EPS);

    }
}