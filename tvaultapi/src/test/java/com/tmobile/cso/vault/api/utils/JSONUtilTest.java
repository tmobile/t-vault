package com.tmobile.cso.vault.api.utils;

import com.tmobile.cso.vault.api.model.UserLogin;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.mockito.InjectMocks;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.context.annotation.ComponentScan;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

@RunWith(PowerMockRunner.class)
@ComponentScan(basePackages={"com.tmobile.cso.vault.api"})
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@PowerMockIgnore({"javax.management.*"})
public class JSONUtilTest {

	@Test
    public void test_getJSON() throws Exception {

        String expectedJsonStr = "{\"username\":\"testuser\",\"password\":\"testuser\"}";
        UserLogin userLogin = new UserLogin("testuser", "testuser");
        String json = JSONUtil.getJSON(userLogin);
        assertEquals(expectedJsonStr, json);
    }

    @Test
    public void test_getJSONasDefaultPrettyPrint() throws Exception {

        String errorJsonStr="{}";
        UserLogin userLogin = new UserLogin("testuser", "testuser");
        String json = JSONUtil.getJSONasDefaultPrettyPrint(userLogin);
        assertNotEquals(errorJsonStr, json);
    }

    @Test
    public void test_getJSONasDefaultPrettyPrintFromString() throws Exception {

        String errorJsonStr="{}";
        String str = "{\"username\":\"testuser\",\"password\":\"testuser\"}";
        String json = JSONUtil.getJSONasDefaultPrettyPrintFromString(str);
        assertNotEquals(errorJsonStr, json);
    }

    @Test
    public void test_getJSONasDefaultPrettyPrintFromString_error() throws Exception {

        String expectedJsonStr="{}";
        String str = "{\"username\":abc\"testuser\",\"password\":\"testuser\"}";
        String json = JSONUtil.getJSONasDefaultPrettyPrintFromString(str);
        assertEquals(expectedJsonStr, json);
    }

    @Test
    public void test_getObj() throws Exception {

        String str = "{\"username\":\"testuser\",\"password\":\"testuser\"}";
        UserLogin userLogin = new UserLogin("testuser", "testuser");
        Object ob = JSONUtil.getObj(str, UserLogin.class);

        assertTrue(UserLogin.class.equals(((UserLogin)ob).getClass()));
    }

}
