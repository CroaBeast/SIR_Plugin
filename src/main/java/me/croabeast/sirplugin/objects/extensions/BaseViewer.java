package me.croabeast.sirplugin.objects.extensions;

/**
 * The class that is a {@link BaseModule} and a {@link RawViewer} for better handling.
 */
public abstract class BaseViewer extends BaseModule implements RawViewer {

    /**
     * It registers the module by using {@link #registerListener()}.
     */
    @Override
    public void registerModule() {
        registerListener();
    }
}
