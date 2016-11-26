package craftedcart.gcisomanager.type;

import java.util.ArrayList;
import java.util.List;

/**
 * @author CraftedCart
 *         Created on 26/11/2016 (DD/MM/YYYY)
 */
public class Tree<T> {

    private Node<T> rootNode;

    public Tree(T rootData) {
        rootNode = new Node<>(rootData);
        rootNode.children = new ArrayList<>();
    }

    public Node<T> getRootNode() {
        return rootNode;
    }

    public static class Node<T> {
        private T data;
        private Node<T> parent;
        private List<Node<T>> children;

        public Node(T data) {
            this.data = data;
            children = new ArrayList<>();
        }

        public T getData() {
            return data;
        }

        public Node<T> getParent() {
            return parent;
        }

        public List<Node<T>> getChildren() {
            return children;
        }

        public void addChild(Node<T> node) {
            children.add(node);
            node.parent = this;
        }

    }

}
