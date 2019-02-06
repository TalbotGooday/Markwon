package ru.noties.markwon.recycler;

import android.support.annotation.NonNull;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import org.commonmark.node.Node;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ru.noties.markwon.Markwon;

class MarkwonAdapterImpl extends MarkwonAdapter {

    private final SparseArray<Entry<Node, Holder>> entries;
    private final Entry<Node, Holder> defaultEntry;
    private final Reducer reducer;

    private LayoutInflater layoutInflater;

    private Markwon markwon;
    private List<Node> nodes;

    @SuppressWarnings("WeakerAccess")
    MarkwonAdapterImpl(
            @NonNull SparseArray<Entry<Node, Holder>> entries,
            @NonNull Entry<Node, Holder> defaultEntry,
            @NonNull Reducer reducer) {
        this.entries = entries;
        this.defaultEntry = defaultEntry;
        this.reducer = reducer;
        setHasStableIds(true);
    }

    @Override
    public void setMarkdown(@NonNull Markwon markwon, @NonNull String markdown) {
        setParsedMarkdown(markwon, markwon.parse(markdown));
    }

    @Override
    public void setParsedMarkdown(@NonNull Markwon markwon, @NonNull Node document) {
        setParsedMarkdown(markwon, reducer.reduce(document));
    }

    @Override
    public void setParsedMarkdown(@NonNull Markwon markwon, @NonNull List<Node> nodes) {
        // clear all entries before applying

        defaultEntry.clear();

        for (int i = 0, size = entries.size(); i < size; i++) {
            entries.valueAt(i).clear();
        }

        this.markwon = markwon;
        this.nodes = nodes;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        if (layoutInflater == null) {
            layoutInflater = LayoutInflater.from(parent.getContext());
        }

        final Entry<Node, Holder> entry = getEntry(viewType);

        return entry.createHolder(layoutInflater, parent);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {

        final Node node = nodes.get(position);
        final int viewType = getNodeViewType(node.getClass());

        final Entry<Node, Holder> entry = getEntry(viewType);

        entry.bindHolder(markwon, holder, node);
    }

    @Override
    public int getItemCount() {
        return nodes != null
                ? nodes.size()
                : 0;
    }

    @SuppressWarnings("unused")
    @NonNull
    public List<Node> getItems() {
        return nodes != null
                ? Collections.unmodifiableList(nodes)
                : Collections.<Node>emptyList();
    }

    @Override
    public int getItemViewType(int position) {
        return getNodeViewType(nodes.get(position).getClass());
    }

    @Override
    public long getItemId(int position) {
        final Node node = nodes.get(position);
        final int type = getNodeViewType(node.getClass());
        final Entry<Node, Holder> entry = getEntry(type);
        return entry.id(node);
    }

    @Override
    public int getNodeViewType(@NonNull Class<? extends Node> node) {
        // if has registered -> then return it, else 0
        final int hash = node.hashCode();
        if (entries.indexOfKey(hash) > -1) {
            return hash;
        }
        return 0;
    }

    @NonNull
    private Entry<Node, Holder> getEntry(int viewType) {
        return viewType == 0
                ? defaultEntry
                : entries.get(viewType);
    }

    static class BuilderImpl implements Builder {

        private final SparseArray<Entry<Node, Holder>> entries = new SparseArray<>(3);

        private Entry<Node, Holder> defaultEntry;
        private Reducer reducer;

        @NonNull
        @Override
        public <N extends Node> Builder include(
                @NonNull Class<N> node,
                @NonNull Entry<? super N, ? extends Holder> entry) {
            //noinspection unchecked
            entries.append(node.hashCode(), (Entry<Node, Holder>) entry);
            return this;
        }

        @NonNull
        @Override
        public Builder defaultEntry(@NonNull Entry<? extends Node, ? extends Holder> defaultEntry) {
            //noinspection unchecked
            this.defaultEntry = (Entry<Node, Holder>) defaultEntry;
            return this;
        }

        @NonNull
        @Override
        public Builder defaultEntry(int layoutResId) {
            //noinspection unchecked
            this.defaultEntry = (Entry<Node, Holder>) (Entry) new SimpleEntry(layoutResId);
            return this;
        }

        @NonNull
        @Override
        public Builder reducer(@NonNull Reducer reducer) {
            this.reducer = reducer;
            return this;
        }

        @NonNull
        @Override
        public MarkwonAdapter build() {

            if (defaultEntry == null) {
                //noinspection unchecked
                defaultEntry = (Entry<Node, Holder>) (Entry) new SimpleEntry();
            }

            if (reducer == null) {
                reducer = new ReducerImpl();
            }

            return new MarkwonAdapterImpl(entries, defaultEntry, reducer);
        }
    }

    static class ReducerImpl implements Reducer {

        @NonNull
        @Override
        public List<Node> reduce(@NonNull Node root) {

            final List<Node> list = new ArrayList<>();

            // we will extract all blocks that are direct children of Document
            Node node = root.getFirstChild();
            Node temp;

            while (node != null) {
                list.add(node);
                temp = node.getNext();
                node.unlink();
                node = temp;
            }

            return list;
        }
    }
}
