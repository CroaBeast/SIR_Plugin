package me.croabeast.sirplugin.objects.extensions;

/**
 * The class that is a {@link SIRModule} and a {@link RawViewer} for better handling.
 */
public abstract class SIRViewer extends SIRModule implements RawViewer {

    /**
     * It registers the module by using {@link #registerListener()}.
     */
    @Override
    public void registerModule() {
        registerListener();
    }
}
