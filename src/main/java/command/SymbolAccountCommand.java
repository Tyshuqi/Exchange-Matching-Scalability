package command;

import javax.xml.bind.annotation.*;

@XmlAccessorType(XmlAccessType.FIELD)
public class SymbolAccountCommand {
    @XmlAttribute
    private String id;

    @XmlValue
    private int shares;
}
