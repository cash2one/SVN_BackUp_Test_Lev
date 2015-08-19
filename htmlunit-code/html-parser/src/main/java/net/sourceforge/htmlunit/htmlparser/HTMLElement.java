package net.sourceforge.htmlunit.htmlparser;

public enum HTMLElement {
    HTML(Type.UNDEFINED),
    HEAD(Type.UNDEFINED, HTML),
    BODY(Type.CONTAINER,  HTML, new HTMLElement[] {HEAD}),

    B(Type.INLINE, BODY),
    H1(Type.BLOCK, BODY),
    I(Type.INLINE, BODY),
    P(Type.CONTAINER, BODY),
    SCRIPT(Type.SPECIAL, HEAD),
    U(Type.INLINE, BODY),
    UNKNOWN(Type.CONTAINER);

    static {
        P.closes_ = new HTMLElement[] {P};
    }

    private enum Type {UNDEFINED, INLINE, BLOCK, EMPTY, CONTAINER, SPECIAL};
    
    private HTMLElement parent_;
    private HTMLElement[] closes_;

    private Type type_;

    HTMLElement(Type flags) {
        this(flags, null);
    }

    HTMLElement(Type flags, HTMLElement parent) {
        this(flags, parent, TagBalancer.EMPTY_ELEMENTS);
    }

    HTMLElement(Type type, HTMLElement parent, HTMLElement[] closes) {
        type_ = type;
        parent_ = parent;
        closes_ = closes;
    }

    public HTMLElement getParent() {
        return parent_;
    }

    public HTMLElement[] getCloses() {
        return closes_;
    }

    public static HTMLElement getElement(String tagName) {
        tagName = tagName.toUpperCase();
        for (HTMLElement e : values()) {
            if (e.name().equals(tagName)) {
                return e;
            }
        }
        return UNKNOWN;
    }

    public boolean isInline() {
        return type_ == Type.INLINE;
    }
}
