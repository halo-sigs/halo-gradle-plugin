package run.halo.gradle.watch;

/**
 * @author guqing
 * @since 2.0.0
 */
class StaticSnapshotStateRepository implements SnapshotStateRepository {

    static final StaticSnapshotStateRepository INSTANCE = new StaticSnapshotStateRepository();

    private volatile Object state;

    @Override
    public void save(Object state) {
        this.state = state;
    }

    @Override
    public Object restore() {
        return this.state;
    }

}
