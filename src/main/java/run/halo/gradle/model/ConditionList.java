package run.halo.gradle.model;

import java.util.AbstractCollection;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Objects;
import java.util.function.Consumer;
import javax.annotation.Nonnull;

/**
 * @see
 * <a href="https://github.com/halo-dev/halo/blob/main/api/src/main/java/run/halo/app/infra/ConditionList.java">ConditionList</a>
 */
public class ConditionList extends AbstractCollection<Condition> {
    private final Deque<Condition> conditions = new LinkedList<>();

    @Override
    public boolean add(@Nonnull Condition condition) {
        if (isSame(conditions.peekFirst(), condition)) {
            return false;
        }
        return conditions.add(condition);
    }

    public boolean addFirst(@Nonnull Condition condition) {
        if (isSame(conditions.peekFirst(), condition)) {
            return false;
        }
        conditions.addFirst(condition);
        return true;
    }

    private Condition getCondition(String type) {
        for (Condition condition : conditions) {
            if (condition.getType().equals(type)) {
                return condition;
            }
        }
        return null;
    }

    public void remove(Condition condition) {
        conditions.remove(condition);
    }

    public Condition peek() {
        return peekFirst();
    }

    public Condition peekFirst() {
        return conditions.peekFirst();
    }

    public Condition removeLast() {
        return conditions.removeLast();
    }

    @Override
    public void clear() {
        conditions.clear();
    }

    public int size() {
        return conditions.size();
    }

    private boolean isSame(Condition a, Condition b) {
        if (a == null || b == null) {
            return false;
        }
        return Objects.equals(a.getType(), b.getType())
            && Objects.equals(a.getReason(), b.getReason())
            && Objects.equals(a.getMessage(), b.getMessage());
    }

    @Override
    @Nonnull
    public Iterator<Condition> iterator() {
        return conditions.iterator();
    }

    @Override
    public void forEach(Consumer<? super Condition> action) {
        conditions.forEach(action);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ConditionList that = (ConditionList) o;
        return Objects.equals(conditions, that.conditions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(conditions);
    }
}
