package org.aksw.jena_sparql_api.concept_cache.dirty;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.aksw.jena_sparql_api.concept_cache.op.TreeUtils;

public class TreeImpl<T>
    implements Tree<T>
{
    protected T root;
    protected Function<T, List<T>> parentToChild;
    protected Map<T, T> childToParent;

    public TreeImpl(T root, Function<T, List<T>> parentToChildren, Map<T, T> childToParent) {
        super();
        this.root = root;
        this.parentToChild = parentToChildren;
        this.childToParent = childToParent;
    }

    @Override
    public T getRoot() {
        return root;
    }

    @Override
    public List<T> getChildren(T node) {
        List<T> result = parentToChild.apply(node);
        return result;
    }

    @Override
    public T getParent(T node) {
        T result = childToParent.get(node);
        return result;
    }

    public static <T> TreeImpl<T> create(T root, Function<T, List<T>> parentToChildren) {
        Map<T, T> childToParent = TreeUtils.parentMap(root, parentToChildren);

        TreeImpl<T> result = new TreeImpl<>(root, parentToChildren, childToParent);
        return result;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((childToParent == null) ? 0 : childToParent.hashCode());
        result = prime * result
                + ((parentToChild == null) ? 0 : parentToChild.hashCode());
        result = prime * result + ((root == null) ? 0 : root.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        TreeImpl<?> other = (TreeImpl<?>) obj;
        if (childToParent == null) {
            if (other.childToParent != null)
                return false;
        } else if (!childToParent.equals(other.childToParent))
            return false;
        if (parentToChild == null) {
            if (other.parentToChild != null)
                return false;
        } else if (!parentToChild.equals(other.parentToChild))
            return false;
        if (root == null) {
            if (other.root != null)
                return false;
        } else if (!root.equals(other.root))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "TreeImpl [root=" + root + ", parentToChild=" + parentToChild
                + ", childToParent=" + childToParent + "]";
    }
    
    
}
