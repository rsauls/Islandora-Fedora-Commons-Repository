
package org.fcrepo.test.fesl.policy;

import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import org.w3c.dom.Document;

import org.fcrepo.server.security.xacml.pdp.data.DbXmlPolicyDataManager;
import org.fcrepo.server.security.xacml.pdp.data.PolicyDataManager;
import org.fcrepo.test.fesl.util.AuthorizationDeniedException;
import org.fcrepo.test.fesl.util.DataUtils;
import org.fcrepo.test.fesl.util.FedoraUtil;
import org.fcrepo.test.fesl.util.HttpUtils;
import org.fcrepo.test.fesl.util.LoadDataset;
import org.fcrepo.test.fesl.util.RemoveDataset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import junit.framework.JUnit4TestAdapter;


public class TestPolicies {

    private static final Logger logger =
            LoggerFactory.getLogger(TestPolicies.class);

    private static final String PROPERTIES = "fedora";

    private static final String RESOURCEBASE =
            "src/test/resources/test-objects";

    private static HttpUtils httpUtils = null;

    private static PolicyDataManager polMan = null;

    public static junit.framework.Test suite() {
        return new JUnit4TestAdapter(TestPolicies.class);
    }

    @BeforeClass
    public static void setup() {
        PropertyResourceBundle prop =
                (PropertyResourceBundle) ResourceBundle.getBundle(PROPERTIES);
        String username = prop.getString("fedora.admin.username");
        String password = prop.getString("fedora.admin.password");
        //String fedoraUrl = prop.getString("fedora.url");
        String fedoraUrl = FedoraUtil.getBaseURL();

        try {
            if (logger.isDebugEnabled()) {
                logger.debug("Setting up...");
            }

            polMan = new DbXmlPolicyDataManager();
            httpUtils = new HttpUtils(fedoraUrl, username, password);

            // Load the admin policy to give us rights to add objects
            String policyId = addPolicy("test-access-admin.xml");

            LoadDataset.main(null);

            // httpUtils.get("/fedora/risearch?flush=true");

            // Now that objects are loaded, remove the policy
            delPolicy(policyId);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            Assert.fail(e.getMessage());
        }
    }

    @AfterClass
    public static void teardown() {
        try {
            if (logger.isDebugEnabled()) {
                logger.debug("Tearing down...");
            }

            polMan = new DbXmlPolicyDataManager();

            // Load the admin policy to give us rights to remove objects
            String policyId = addPolicy("test-access-admin.xml");

            RemoveDataset.main(null);

            // Now that objects are loaded, remove the policy
            delPolicy(policyId);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            Assert.fail(e.getMessage());
        }
    }

    @Test(expected = AuthorizationDeniedException.class)
    public void testAdminGetDeny() throws Exception {
        // getting object test:1000007 but applying policy
        // to parent object (test:1000006) first

        String policyId = addPolicy("test-policy-00.xml");

        try {
            String url = "/fedora/objects/test:1000007?format=xml";
            String response = httpUtils.get(url);
            if (logger.isDebugEnabled()) {
                logger.debug("http response:\n" + response);
            }

            // If we get here, we fail... should have thrown exception
            Assert.fail();
        } catch (Exception e) {
            throw e;
        } finally {
            delPolicy(policyId);
        }
    }

    @Test
    public void testAdminGetPermit() throws Exception {
        // getting object test:1000007 but applying policy
        // to parent object (test:1000006) first

        String policyId = addPolicy("test-policy-01.xml");

        try {
            String url = "/fedora/objects/test:1000007?format=xml";
            String response = httpUtils.get(url);
            if (logger.isDebugEnabled()) {
                logger.debug("http response:\n" + response);
            }

            boolean check = response.contains("<objLabel>Dexter</objLabel>");
            Assert.assertTrue("Expected object data not found", check);
        } catch (Exception e) {
            throw e;
        } finally {
            delPolicy(policyId);
        }
    }

    private static String getPolicyId(byte[] data) throws Exception {
        Document doc = DataUtils.getDocumentFromBytes(data);
        String pid = doc.getDocumentElement().getAttribute("PolicyId");

        return pid;
    }

    private static String addPolicy(String policyName) throws Exception {
        byte[] policy =
                DataUtils.loadFile(RESOURCEBASE + "/xacml/" + policyName);
        String policyId = getPolicyId(policy);
        polMan.addPolicy(new String(policy), policyId);
        Thread.sleep(1000);

        return policyId;
    }

    private static void delPolicy(String policyId) throws Exception {
        polMan.deletePolicy(policyId);
        Thread.sleep(1000);
    }
}
