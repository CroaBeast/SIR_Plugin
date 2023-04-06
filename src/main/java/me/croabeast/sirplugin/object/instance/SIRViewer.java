package me.croabeast.sirplugin.object.instance;

/**
 * The class that is a {@link SIRModule} and a {@link SIRListener} for better handling.
 */
public abstract class SIRViewer extends SIRModule implements SIRListener {

    public SIRViewer(String name) {
        super(name);
    }

    /**
     * It registers the module by using {@link #register()}.
     */
    @Override
    public void registerModule() {
        register();
    }
}
