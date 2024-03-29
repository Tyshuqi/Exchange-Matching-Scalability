package command;

import javax.xml.bind.annotation.*;

@XmlAccessorType(XmlAccessType.FIELD)
public class OrderCommand {
    @XmlAttribute(name = "sym")
    private String sym;

    @XmlAttribute(name = "amount")
    private int amount;

    @XmlAttribute(name = "limit")
    private double limit;
}
