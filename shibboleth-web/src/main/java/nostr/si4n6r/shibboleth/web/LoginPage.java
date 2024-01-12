package nostr.si4n6r.shibboleth.web;

import nostr.si4n6r.shibboleth.web.components.LoginForm;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.form.Form;

import java.io.Serial;

public class LoginPage extends WebPage {
    @Serial
    private static final long serialVersionUID = 1L;

    public LoginPage() {
        Form<?> form = new LoginForm("loginForm");
        add(form);
    }
}
