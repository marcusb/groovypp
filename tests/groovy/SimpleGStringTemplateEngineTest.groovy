package groovy;


/**
 * @author andy
 * @since Jan 11, 2006 1:05:23 PM
 */
public class SimpleGStringTemplateEngineTest extends GroovyShellTestCase
{
  public void testRegressionCommentBug() throws Exception
  {
        shell.evaluate  """

        @Typed
        def u() {
            final groovy.text.Template template = new groovy.text.GStringTemplateEngine().createTemplate(
                "<% // This is a comment that will be filtered from output %>\\n" +
                "Hello World!"
            );

            final StringWriter sw = new StringWriter();
            template.make().writeTo(sw);
            assert "\\nHello World!" == sw.toString();
        }

        u()
      """
  }
}
