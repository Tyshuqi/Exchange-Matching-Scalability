package command;

import javax.xml.bind.annotation.*;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
public class SymbolCommand {
    @XmlAttribute(name = "sym")
    private String sym;

    @XmlElement(name = "account")
    private List<SymbolAccountCommand> accounts;
}
