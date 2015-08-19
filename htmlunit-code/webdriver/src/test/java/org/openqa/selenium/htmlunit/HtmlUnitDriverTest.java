package org.openqa.selenium.htmlunit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.net.URL;

import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class HtmlUnitDriverTest {

	@Test
	public void test() throws Exception {
		WebDriver driver = new HtmlUnitDriver();
        final String resource = "xhtmlTest.html";
        final URL url = getClass().getClassLoader().getResource(resource);
        assertNotNull(url);

		driver.get(url.toExternalForm());
		WebElement a = driver.findElement(By.id("id1"));
		assertEquals(a.toString(), "<a id=\"id1\" href=\"#\">");
	}
}
