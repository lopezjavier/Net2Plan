/*******************************************************************************
 * Copyright (c) 2015 Pablo Pavon Mariño.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * <p>
 * Contributors:
 * Pablo Pavon Mariño - initial API and implementation
 ******************************************************************************/


package com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.specificTables;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.net2plan.gui.plugins.GUINetworkDesign;
import com.net2plan.gui.plugins.networkDesign.CellRenderers;
import com.net2plan.gui.plugins.networkDesign.ElementSelection;
import com.net2plan.gui.plugins.networkDesign.interfaces.ITableRowFilter;
import com.net2plan.gui.plugins.networkDesign.interfaces.ITopologyCanvas;
import com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.tableVisualizationFilters.TBFToFromCarriedTraffic;
import com.net2plan.gui.plugins.networkDesign.visualizationControl.VisualizationState;
import com.net2plan.gui.plugins.networkDesign.whatIfAnalysisPane.WhatIfAnalysisPane;
import com.net2plan.gui.utils.ClassAwareTableModel;
import com.net2plan.gui.utils.WiderJComboBox;
import com.net2plan.interfaces.networkDesign.*;
import com.net2plan.internal.Constants.NetworkElementType;
import com.net2plan.internal.ErrorHandling;
import com.net2plan.utils.CollectionUtils;
import com.net2plan.utils.Pair;
import com.net2plan.utils.StringUtils;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.collections15.BidiMap;

import javax.swing.*;
import javax.swing.table.TableModel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.util.*;

/**
 */
@SuppressWarnings("unchecked")
public class AdvancedJTable_node extends AdvancedJTable_networkElement
{
    public static final int COLUMN_ID = 0;
    public static final int COLUMN_INDEX = 1;
    public static final int COLUMN_SHOWHIDE = 2;
    public static final int COLUMN_NAME = 3;
    public static final int COLUMN_STATE = 4;
    public static final int COLUMN_XCOORD = 5;
    public static final int COLUMN_YCOORD = 6;
    public static final int COLUMN_OUTLINKS = 7;
    public static final int COLUMN_INLINKS = 8;
    public static final int COLUMN_INGRESSTRAFFIC = 9;
    public static final int COLUMN_EGRESSTRAFFIC = 10;
    public static final int COLUMN_INGRESSMULTICASTTRAFFIC = 11;
    public static final int COLUMN_EGRESSMULTICASTTRAFFIC = 12;
    public static final int COLUMN_INCOMINGLINKTRAFFIC = 13;
    public static final int COLUMN_OUTGOINGLINKTRAFFIC = 14;
    public static final int COLUMN_SRGS = 15;
    public static final int COLUMN_POPULATION = 16;
    public static final int COLUMN_TAGS = 17;
    public static final int COLUMN_ATTRIBUTES = 18;
    private static final String netPlanViewTabName = "Nodes";
    private static final String[] netPlanViewTableHeader = StringUtils.arrayOf("Unique identifier", "Index", "Show/Hide", "Name",
            "State", "xCoord / Longitude", "yCoord / Latitude", "Outgoing links", "Incoming links",
            "Ingress traffic", "Egress traffic", "Incoming traffic", "Outgoing traffic", "Ingress traffic (multicast)", "Egress traffic (multicast)", "SRGs", "Population", "Tags", "Attributes");
    private static final String[] netPlanViewTableTips = StringUtils.arrayOf("Unique identifier (never repeated in the same netPlan object, never changes, long)",
            "Index (consecutive integer starting in zero)",
            "Indicates whether or not the node is visible in the topology canvas",
            "Node name", "Indicates whether the node is in up/down state", "Coordinate along x-axis (i.e. longitude)",
            "Coordinate along y-axis (i.e. latitude)", "Outgoing links", "Incoming links",
            "Total UNICAST traffic entering to the network from this node (offered / carried)",
            "Total UNICAST traffic of demands ending in this node (offered / carried)",
            "Total MULTICAST traffic entering to the network from this node",
            "Total MULTICAST traffic leaving the network from this node",
            "Total traffic (unicast and multicast) in the node input links",
            "Total traffic (unicast and multicast) in the node output links",
            "SRGs including this node", "Total population in this node", "Node-specific tags", "Node-specific attributes");
    private boolean updateVisualization = true;

    /**
     * Default constructor.
     *
     * @param callback The network callback
     * @since 0.2.0
     */
    public AdvancedJTable_node(final GUINetworkDesign callback)
    {
        super(createTableModel(callback), callback, NetworkElementType.NODE, true);
        this.updateVisualization = true;
        setDefaultCellRenderers(callback);
        setSpecificCellRenderers();
        setColumnRowSortingFixedAndNonFixedTable();
        fixedTable.setDefaultRenderer(Boolean.class, this.getDefaultRenderer(Boolean.class));
        fixedTable.setDefaultRenderer(Double.class, this.getDefaultRenderer(Double.class));
        fixedTable.setDefaultRenderer(Object.class, this.getDefaultRenderer(Object.class));
        fixedTable.setDefaultRenderer(Float.class, this.getDefaultRenderer(Float.class));
        fixedTable.setDefaultRenderer(Long.class, this.getDefaultRenderer(Long.class));
        fixedTable.setDefaultRenderer(Integer.class, this.getDefaultRenderer(Integer.class));
        fixedTable.setDefaultRenderer(String.class, this.getDefaultRenderer(String.class));
        fixedTable.getTableHeader().setDefaultRenderer(new CellRenderers.FixedTableHeaderRenderer());
    }


    public List<Object[]> getAllData(NetPlan currentState, ArrayList<String> attributesTitles)
    {
        final List<Node> rowVisibleNodes = getVisibleElementsInTable();
        List<Object[]> allNodeData = new LinkedList<Object[]>();


        for (Node node : rowVisibleNodes)
        {
            Set<Link> outgoingLinks = node.getOutgoingLinks();
            Set<Link> incomingLinks = node.getIncomingLinks();

            Object[] nodeData = new Object[netPlanViewTableHeader.length + attributesTitles.size()];
            nodeData[COLUMN_ID] = node.getId();
            nodeData[COLUMN_INDEX] = node.getIndex();
            nodeData[COLUMN_SHOWHIDE] = !callback.getVisualizationState().isHiddenOnCanvas(node);
            nodeData[COLUMN_NAME] = node.getName();
            nodeData[COLUMN_STATE] = node.isUp();
            nodeData[COLUMN_XCOORD] = node.getXYPositionMap().getX();
            nodeData[COLUMN_YCOORD] = node.getXYPositionMap().getY();
            nodeData[COLUMN_OUTLINKS] = outgoingLinks.isEmpty() ? "none" : outgoingLinks.size() + " (" + CollectionUtils.join(outgoingLinks, ", ") + ")";
            nodeData[COLUMN_INLINKS] = incomingLinks.isEmpty() ? "none" : incomingLinks.size() + " (" + CollectionUtils.join(incomingLinks, ", ") + ")";
            nodeData[COLUMN_INGRESSTRAFFIC] = node.getIngressOfferedTraffic() + "(" + node.getIngressCarriedTraffic() + ")";
            nodeData[COLUMN_EGRESSTRAFFIC] = node.getEgressOfferedTraffic() + "(" + node.getEgressCarriedTraffic() + ")";
            nodeData[COLUMN_INCOMINGLINKTRAFFIC] = node.getIncomingLinksTraffic();
            nodeData[COLUMN_OUTGOINGLINKTRAFFIC] = node.getOutgoingLinksTraffic();
            nodeData[COLUMN_INGRESSMULTICASTTRAFFIC] = node.getIngressOfferedMulticastTraffic() + "(" + node.getIngressOfferedMulticastTraffic() + ")";
            nodeData[COLUMN_EGRESSMULTICASTTRAFFIC] = node.getEgressOfferedMulticastTraffic() + "(" + node.getEgressOfferedMulticastTraffic() + ")";
            nodeData[COLUMN_SRGS] = node.getSRGs().isEmpty() ? "none" : node.getSRGs().size() + " (" + CollectionUtils.join(currentState.getIndexes(node.getSRGs()), ", ") + ")";
            nodeData[COLUMN_POPULATION] = node.getPopulation();
            nodeData[COLUMN_TAGS] = StringUtils.listToString(Lists.newArrayList(node.getTags()));
            nodeData[COLUMN_ATTRIBUTES] = StringUtils.mapToString(node.getAttributes());
            for (int i = netPlanViewTableHeader.length; i < netPlanViewTableHeader.length + attributesTitles.size(); i++)
            {
                if (node.getAttributes().containsKey(attributesTitles.get(i - netPlanViewTableHeader.length)))
                {
                    nodeData[i] = node.getAttribute(attributesTitles.get(i - netPlanViewTableHeader.length));
                }
            }

            allNodeData.add(nodeData);
        }
        
        /* Add the aggregation row with the aggregated statistics */
        final double aggIngress = rowVisibleNodes.stream().mapToDouble(e -> e.getIngressOfferedTraffic()).sum();
        final double aggEgress = rowVisibleNodes.stream().mapToDouble(e -> e.getEgressOfferedTraffic()).sum();
        final double aggIncomingLinksTraffic = rowVisibleNodes.stream().mapToDouble(e -> e.getIncomingLinksTraffic()).sum();
        final double aggOutgoingLinksTraffic = rowVisibleNodes.stream().mapToDouble(e -> e.getOutgoingLinksTraffic()).sum();
        final double aggMIngress = rowVisibleNodes.stream().mapToDouble(e -> e.getIngressOfferedMulticastTraffic()).sum();
        final double aggMEgress = rowVisibleNodes.stream().mapToDouble(e -> e.getEgressOfferedMulticastTraffic()).sum();
        final LastRowAggregatedValue[] aggregatedData = new LastRowAggregatedValue[netPlanViewTableHeader.length + attributesTitles.size()];
        Arrays.fill(aggregatedData, new LastRowAggregatedValue());
        aggregatedData[COLUMN_INGRESSTRAFFIC] = new LastRowAggregatedValue(aggIngress);
        aggregatedData[COLUMN_EGRESSTRAFFIC] = new LastRowAggregatedValue(aggEgress);
        aggregatedData[COLUMN_INCOMINGLINKTRAFFIC] = new LastRowAggregatedValue(aggIncomingLinksTraffic);
        aggregatedData[COLUMN_OUTGOINGLINKTRAFFIC] = new LastRowAggregatedValue(aggOutgoingLinksTraffic);
        aggregatedData[COLUMN_INGRESSMULTICASTTRAFFIC] = new LastRowAggregatedValue(aggMIngress);
        aggregatedData[COLUMN_EGRESSMULTICASTTRAFFIC] = new LastRowAggregatedValue(aggMEgress);
        allNodeData.add(aggregatedData);

        return allNodeData;
    }

    public String[] getCurrentTableHeaders()
    {

        ArrayList<String> attColumnsHeaders = getAttributesColumnsHeaders();
        String[] headers = new String[netPlanViewTableHeader.length + attColumnsHeaders.size()];
        for (int i = 0; i < headers.length; i++)
        {
            if (i < netPlanViewTableHeader.length)
            {
                headers[i] = netPlanViewTableHeader[i];
            } else
            {
                headers[i] = "Att: " + attColumnsHeaders.get(i - netPlanViewTableHeader.length);
            }
        }


        return headers;
    }


    public String getTabName()
    {
        return netPlanViewTabName;
    }

    public String[] getTableHeaders()
    {
        return netPlanViewTableHeader;
    }

    public String[] getTableTips()
    {
        return netPlanViewTableTips;
    }

    public boolean hasElements()
    {
        final ITableRowFilter rf = callback.getVisualizationState().getTableRowFilter();
        final NetworkLayer layer = callback.getDesign().getNetworkLayerDefault();
        return rf == null ? callback.getDesign().hasNodes() : rf.hasNodes(layer);
    }

    @Override
    public int getAttributesColumnIndex()
    {
        return COLUMN_ATTRIBUTES;
    }

    @Override
    public ArrayList<String> getAttributesColumnsHeaders()
    {
        ArrayList<String> attColumnsHeaders = new ArrayList<>();
        for (Node node : getVisibleElementsInTable())
            for (Map.Entry<String, String> entry : node.getAttributes().entrySet())
                if (attColumnsHeaders.contains(entry.getKey()) == false)
                    attColumnsHeaders.add(entry.getKey());
        return attColumnsHeaders;
    }

    private static TableModel createTableModel(final GUINetworkDesign callback)
    {
        TableModel nodeTableModel = new ClassAwareTableModel(new Object[1][netPlanViewTableHeader.length], netPlanViewTableHeader)
        {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isCellEditable(int rowIndex, int columnIndex)
            {
                if (!callback.getVisualizationState().isNetPlanEditable()) return false;
                if (columnIndex >= netPlanViewTableHeader.length) return true;
                if (rowIndex == getRowCount() - 1) return false;
                if (getValueAt(rowIndex, columnIndex) == null) return false;

                return columnIndex == COLUMN_SHOWHIDE || columnIndex == COLUMN_NAME || columnIndex == COLUMN_STATE || columnIndex == COLUMN_XCOORD
                        || columnIndex == COLUMN_YCOORD || columnIndex == COLUMN_POPULATION;
            }

            @Override
            public void setValueAt(Object newValue, int row, int column)
            {
                Object oldValue = getValueAt(row, column);

				/* If value doesn't change, exit from function */
                if (newValue != null && newValue.equals(oldValue)) return;

                NetPlan netPlan = callback.getDesign();

                if (getValueAt(row, 0) == null) row = row - 1;
                final long nodeId = (Long) getValueAt(row, 0);
                final Node node = netPlan.getNodeFromId(nodeId);
                try
                {
                    switch (column)
                    {
                        case COLUMN_SHOWHIDE:
                            if (newValue == null) return;
                            if (!(Boolean) newValue)
                            {
                                callback.getVisualizationState().hideOnCanvas(node);
                            } else
                            {
                                callback.getVisualizationState().showOnCanvas(node);
                            }
                            callback.getVisualizationState().pickNode(node);
                            callback.updateVisualizationAfterChanges(Sets.newHashSet(NetworkElementType.NODE));
                            callback.addNetPlanChange();
                            break;

                        case COLUMN_NAME:
                            node.setName(newValue.toString());
                            callback.getVisualizationState().pickNode(node);
                            callback.updateVisualizationAfterChanges(Sets.newHashSet(NetworkElementType.NODE));
                            callback.addNetPlanChange();
                            break;

                        case COLUMN_STATE:
                            final boolean isNodeUp = (Boolean) newValue;
                            if (callback.getVisualizationState().isWhatIfAnalysisActive())
                            {
                                final WhatIfAnalysisPane whatIfPane = callback.getWhatIfAnalysisPane();
                                synchronized (whatIfPane)
                                {
                                    whatIfPane.whatIfLinkNodesFailureStateChanged(isNodeUp ? Sets.newHashSet(node) : null, isNodeUp ? null : Sets.newHashSet(node), null, null);
                                    if (whatIfPane.getLastWhatIfExecutionException() != null)
                                        throw whatIfPane.getLastWhatIfExecutionException();
                                    whatIfPane.wait(); // wait until the simulation ends
                                    if (whatIfPane.getLastWhatIfExecutionException() != null)
                                        throw whatIfPane.getLastWhatIfExecutionException();

                                    final VisualizationState vs = callback.getVisualizationState();
                                    Pair<BidiMap<NetworkLayer, Integer>, Map<NetworkLayer, Boolean>> res =
                                            vs.suggestCanvasUpdatedVisualizationLayerInfoForNewDesign(new HashSet<>(callback.getDesign().getNetworkLayers()));
                                    vs.setCanvasLayerVisibilityAndOrder(callback.getDesign(), res.getFirst(), res.getSecond());
                                    callback.updateVisualizationAfterNewTopology();
                                }
                            } else
                            {
                                node.setFailureState(isNodeUp);
                                callback.updateVisualizationAfterChanges(Sets.newHashSet(NetworkElementType.NODE));
                                callback.getVisualizationState().pickNode(node);
                                callback.updateVisualizationAfterPick();
                                callback.addNetPlanChange();
                            }
                            break;

                        case COLUMN_XCOORD:
                        case COLUMN_YCOORD:
                            Point2D newPosition = column == COLUMN_XCOORD ?
                                    new Point2D.Double(Double.parseDouble(newValue.toString()), node.getXYPositionMap().getY()) :
                                    new Point2D.Double(node.getXYPositionMap().getX(), Double.parseDouble(newValue.toString()));
                            node.setXYPositionMap(newPosition);
                            callback.updateVisualizationAfterChanges(Sets.newHashSet(NetworkElementType.NODE));
                            callback.getVisualizationState().pickNode(node);
                            callback.updateVisualizationAfterPick();
                            callback.addNetPlanChange();
                            break;

                        case COLUMN_POPULATION:
                            if (newValue == null) return;
                            String text = newValue.toString();
                            double value = Double.parseDouble(text);

                            node.setPopulation(value);
                            callback.updateVisualizationAfterChanges(Collections.singleton(NetworkElementType.NODE));
                            callback.getVisualizationState().pickNode(node);
                            callback.addNetPlanChange();
                        default:
                            break;
                    }
                } catch (Throwable ex)
                {
                    ex.printStackTrace();
                    ErrorHandling.showErrorDialog(ex.getMessage(), "Error modifying node");
                    return;
                }

				/* Set new value */
                super.setValueAt(newValue, row, column);
            }
        };

        return nodeTableModel;
    }

    private void setDefaultCellRenderers(final GUINetworkDesign callback)
    {
        setDefaultRenderer(Boolean.class, new CellRenderers.CheckBoxRenderer());
        setDefaultRenderer(Double.class, new CellRenderers.NumberCellRenderer());
        setDefaultRenderer(Object.class, new CellRenderers.NonEditableCellRenderer());
        setDefaultRenderer(Float.class, new CellRenderers.NumberCellRenderer());
        setDefaultRenderer(Long.class, new CellRenderers.NumberCellRenderer());
        setDefaultRenderer(Integer.class, new CellRenderers.NumberCellRenderer());
        setDefaultRenderer(String.class, new CellRenderers.NonEditableCellRenderer());

        setDefaultRenderer(Boolean.class, new CellRenderers.UpDownRenderer(getDefaultRenderer(Boolean.class), callback, NetworkElementType.NODE));
        setDefaultRenderer(Double.class, new CellRenderers.UpDownRenderer(getDefaultRenderer(Double.class), callback, NetworkElementType.NODE));
        setDefaultRenderer(Object.class, new CellRenderers.UpDownRenderer(getDefaultRenderer(Object.class), callback, NetworkElementType.NODE));
        setDefaultRenderer(Float.class, new CellRenderers.UpDownRenderer(getDefaultRenderer(Float.class), callback, NetworkElementType.NODE));
        setDefaultRenderer(Long.class, new CellRenderers.UpDownRenderer(getDefaultRenderer(Long.class), callback, NetworkElementType.NODE));
        setDefaultRenderer(Integer.class, new CellRenderers.UpDownRenderer(getDefaultRenderer(Integer.class), callback, NetworkElementType.NODE));
        setDefaultRenderer(String.class, new CellRenderers.UpDownRenderer(getDefaultRenderer(String.class), callback, NetworkElementType.NODE));
    }

    private void setSpecificCellRenderers()
    {
    }

    @Override
    public void setColumnRowSortingFixedAndNonFixedTable()
    {
        setAutoCreateRowSorter(true);
        final Set<Integer> columnsWithDoubleAndThenParenthesis = Sets.newHashSet(COLUMN_OUTLINKS, COLUMN_INLINKS, COLUMN_INGRESSTRAFFIC, COLUMN_EGRESSTRAFFIC, COLUMN_INGRESSMULTICASTTRAFFIC, COLUMN_EGRESSMULTICASTTRAFFIC);
        DefaultRowSorter rowSorter = ((DefaultRowSorter) getRowSorter());
        for (int col = 0; col <= COLUMN_ATTRIBUTES; col++)
            rowSorter.setComparator(col, new AdvancedJTable_networkElement.ColumnComparator(rowSorter, columnsWithDoubleAndThenParenthesis.contains(col)));
        fixedTable.setAutoCreateRowSorter(true);
        fixedTable.setRowSorter(this.getRowSorter());
        rowSorter = ((DefaultRowSorter) fixedTable.getRowSorter());
        for (int col = 0; col <= COLUMN_ATTRIBUTES; col++)
            rowSorter.setComparator(col, new AdvancedJTable_networkElement.ColumnComparator(rowSorter, columnsWithDoubleAndThenParenthesis.contains(col)));
    }

    public int getNumberOfDecoratorColumns()
    {
        return 2;
    }


    @Override
    protected void doPopup(final MouseEvent e, final int row, ElementSelection selection)
    {
        final JPopupMenu popup = new JPopupMenu();

        if (selection.getSelectionType() != ElementSelection.SelectionType.EMPTY)
        {
            if (selection.getElementType() != NetworkElementType.NODE)
                throw new RuntimeException("Unmatched selected items with table, selected items are of type: " + selection.getElementType());
        }

        final List<Node> rowsInTheTable = this.getVisibleElementsInTable(); // Only visible rows
        final List<Node> selectedNodes = (List<Node>) selection.getNetworkElements();

        /* Add the popup menu option of the filters */
        final JMenu submenuFilters = new JMenu("Filters");
        if (!selectedNodes.isEmpty())
        {
            final JMenuItem filterKeepElementsAffectedThisLayer = new JMenuItem("This layer: Keep elements associated to this node traffic");
            final JMenuItem filterKeepElementsAffectedAllLayers = new JMenuItem("All layers: Keep elements associated to this node traffic");

            submenuFilters.add(filterKeepElementsAffectedThisLayer);
            if (callback.getDesign().getNumberOfLayers() > 1) submenuFilters.add(filterKeepElementsAffectedAllLayers);

            filterKeepElementsAffectedThisLayer.addActionListener(e1 ->
            {
                if (selectedNodes.size() > 1) throw new RuntimeException();
                TBFToFromCarriedTraffic filter = new TBFToFromCarriedTraffic(selectedNodes.get(0), callback.getDesign().getNetworkLayerDefault(), true);
                callback.getVisualizationState().updateTableRowFilter(filter);
                callback.updateVisualizationJustTables();
            });
            filterKeepElementsAffectedAllLayers.addActionListener(e1 ->
            {
                if (selectedNodes.size() > 1) throw new RuntimeException();
                TBFToFromCarriedTraffic filter = new TBFToFromCarriedTraffic(selectedNodes.get(0), callback.getDesign().getNetworkLayerDefault(), false);
                callback.getVisualizationState().updateTableRowFilter(filter);
                callback.updateVisualizationJustTables();
            });
        }

        final JMenuItem tagFilter = new JMenuItem("This layer: Keep elements of tag...");
        tagFilter.addActionListener(e1 -> dialogToFilterByTag(true));
        submenuFilters.add(tagFilter);

        final JMenuItem tagFilterAllLayers = new JMenuItem("All layers: Keep elements of tag...");
        tagFilterAllLayers.addActionListener(e1 -> dialogToFilterByTag(false));
        submenuFilters.add(tagFilterAllLayers);

        popup.add(submenuFilters);

        popup.addSeparator();

        // Popup buttons
        if (callback.getVisualizationState().isNetPlanEditable())
        {
            popup.add(this.getAddOption());

            for (JComponent item : getExtraAddOptions()) popup.add(item);

            if (!rowsInTheTable.isEmpty())
            {
                if (!selectedNodes.isEmpty())
                {
                    JMenuItem removeItem = new JMenuItem("Remove selected" + networkElementType);
                    removeItem.addActionListener(new ActionListener()
                    {
                        @Override
                        public void actionPerformed(ActionEvent e)
                        {
                            try
                            {
                                for (Node selectedNode : selectedNodes) selectedNode.remove();
                                callback.getVisualizationState().recomputeCanvasTopologyBecauseOfLinkOrNodeAdditionsOrRemovals();
                                callback.updateVisualizationAfterChanges(Sets.newHashSet(NetworkElementType.NODE));
                                callback.addNetPlanChange();
                            } catch (Throwable ex)
                            {
                                ErrorHandling.addErrorOrException(ex, getClass());
                                ErrorHandling.showErrorDialog("Unable to remove " + networkElementType);
                            }
                        }
                    });

                    popup.add(removeItem);
                    popup.addSeparator();
                }

                List<JComponent> forcedOptions = getForcedOptions();
                if (!forcedOptions.isEmpty())
                {
                    if (popup.getSubElements().length > 0) popup.addSeparator();
                    for (JComponent item : forcedOptions) popup.add(item);
                }

                if (popup.getSubElements().length > 0) popup.addSeparator();

                JMenuItem removeAllNodesFilteredOut = new JMenuItem("Remove all filtered out nodes");
                removeAllNodesFilteredOut.addActionListener(e1 ->
                {
                    NetPlan netPlan = callback.getDesign();
                    try
                    {
                        for (Node n : new ArrayList<>(netPlan.getNodes()))
                            if (!rowsInTheTable.contains(n)) n.remove();
                        callback.getVisualizationState().recomputeCanvasTopologyBecauseOfLinkOrNodeAdditionsOrRemovals();
                        callback.updateVisualizationAfterChanges(Sets.newHashSet(NetworkElementType.NODE));
                        callback.addNetPlanChange();
                    } catch (Throwable ex)
                    {
                        ex.printStackTrace();
                        ErrorHandling.showErrorDialog(ex.getMessage(), "Unable to complete this action");
                    }
                });
                popup.add(removeAllNodesFilteredOut);

                JMenuItem hideAllNodesFilteredOut = new JMenuItem("Hide all filtered out nodes");
                hideAllNodesFilteredOut.addActionListener(e1 ->
                {
                    Set<Node> rowsInTheTableSet = new HashSet<>(rowsInTheTable);
                    for (Node n : callback.getDesign().getNodes())
                        if (!rowsInTheTableSet.contains(n))
                            callback.getVisualizationState().hideOnCanvas(n);
                    callback.updateVisualizationAfterChanges(Sets.newHashSet(NetworkElementType.NODE));
                    callback.addNetPlanChange();
                });
                popup.add(hideAllNodesFilteredOut);

                addPopupMenuAttributeOptions(e, row, selection, popup);

                List<JComponent> extraOptions = getExtraOptions(selection);
                if (!extraOptions.isEmpty())
                {
                    if (popup.getSubElements().length > 0) popup.addSeparator();
                    for (JComponent item : extraOptions) popup.add(item);
                }
            }
        }

        popup.show(e.getComponent(), e.getX(), e.getY());
    }

    @Override
    protected void showInCanvas(MouseEvent e, ElementSelection selection)
    {
        if (getVisibleElementsInTable().isEmpty()) return;
        if (selection.getElementType() != NetworkElementType.NODE)
            throw new RuntimeException("Unmatched selected items with table, selected items are of type: " + selection.getElementType());

        callback.getVisualizationState().pickNode((List<Node>) selection.getNetworkElements());
        callback.updateVisualizationAfterPick();
    }

    private JMenuItem getAddOption()
    {
        JMenuItem addItem = new JMenuItem("Add " + networkElementType);
        addItem.addActionListener(e ->
        {
            NetPlan netPlan = callback.getDesign();

            try
            {
                Node node = netPlan.addNode(0, 0, "Node " + netPlan.getNumberOfNodes(), null);
                callback.getVisualizationState().recomputeCanvasTopologyBecauseOfLinkOrNodeAdditionsOrRemovals();
                callback.getVisualizationState().pickNode(node);
                callback.updateVisualizationAfterChanges(Sets.newHashSet(NetworkElementType.NODE));
                callback.addNetPlanChange();

                if (networkElementType == NetworkElementType.NODE)
                    callback.runCanvasOperation(ITopologyCanvas.CanvasOperation.ZOOM_ALL);
            } catch (Throwable ex)
            {
                ErrorHandling.showErrorDialog(ex.getMessage(), "Unable to add " + networkElementType);
            }
        });
        return addItem;
    }

    @Override
    protected List<JComponent> getExtraAddOptions()
    {
        return new LinkedList<>();
    }

    @Override
    protected List<JComponent> getExtraOptions(final ElementSelection selection)
    {
        final List<Node> selectedNodes = (List<Node>) selection.getNetworkElements();

        List<JComponent> options = new LinkedList<>();

        if (!selectedNodes.isEmpty())
        {
            JMenuItem switchCoordinates = new JMenuItem("Switch node coordinates from (x,y) to (y,x)");

            switchCoordinates.addActionListener(e ->
            {
                for (Node node : selectedNodes)
                {
                    Point2D currentPosition = node.getXYPositionMap();
                    node.setXYPositionMap(new Point2D.Double(currentPosition.getY(), currentPosition.getX()));
                }

                callback.updateVisualizationAfterChanges(Sets.newHashSet(NetworkElementType.NODE));
                callback.runCanvasOperation(ITopologyCanvas.CanvasOperation.ZOOM_ALL);
                callback.addNetPlanChange();
            });

            options.add(switchCoordinates);

            JMenuItem xyPositionFromAttributes = new JMenuItem("Set node coordinates from attributes");

            xyPositionFromAttributes.addActionListener(e ->
            {
                Set<String> attributeSet = new LinkedHashSet<>();
                for (Node selectedNode : selectedNodes)
                    attributeSet.addAll(selectedNode.getAttributes().keySet());

                try
                {
                    if (attributeSet.isEmpty()) throw new Exception("No attribute to select");

                    final JComboBox latSelector = new WiderJComboBox();
                    final JComboBox lonSelector = new WiderJComboBox();
                    for (String attribute : attributeSet)
                    {
                        latSelector.addItem(attribute);
                        lonSelector.addItem(attribute);
                    }

                    JPanel pane = new JPanel(new MigLayout("", "[][grow]", "[][]"));
                    pane.add(new JLabel("X-coordinate / Longitude: "));
                    pane.add(lonSelector, "growx, wrap");
                    pane.add(new JLabel("Y-coordinate / Latitude: "));
                    pane.add(latSelector, "growx, wrap");

                    int result = JOptionPane.showConfirmDialog(null, pane, "Please select the attributes for coordinates", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
                    if (result != JOptionPane.OK_OPTION) return;

                    String latAttribute = latSelector.getSelectedItem().toString();
                    String lonAttribute = lonSelector.getSelectedItem().toString();

                    for (Node node : selectedNodes)
                        node.setXYPositionMap(new Point2D.Double(Double.parseDouble(node.getAttribute(lonAttribute)), Double.parseDouble(node.getAttribute(latAttribute))));

                    callback.updateVisualizationAfterChanges(Sets.newHashSet(NetworkElementType.NODE));
                    callback.addNetPlanChange();

                    callback.runCanvasOperation(ITopologyCanvas.CanvasOperation.ZOOM_ALL);
                } catch (Throwable ex)
                {
                    ErrorHandling.showErrorDialog(ex.getMessage(), "Error retrieving coordinates from attributes");
                }
            });
            options.add(xyPositionFromAttributes);

            JMenuItem nameFromAttribute = new JMenuItem("Set node name from attribute");
            nameFromAttribute.addActionListener(e ->
            {
                Set<String> attributeSet = new LinkedHashSet<String>();
                for (Node selectedNode : selectedNodes)
                    attributeSet.addAll(selectedNode.getAttributes().keySet());

                try
                {
                    if (attributeSet.isEmpty()) throw new Exception("No attribute to select");

                    final JComboBox selector = new WiderJComboBox();
                    for (String attribute : attributeSet)
                        selector.addItem(attribute);

                    JPanel pane = new JPanel(new MigLayout("", "[][grow]", "[]"));
                    pane.add(new JLabel("Name: "));
                    pane.add(selector, "growx, wrap");

                    int result = JOptionPane.showConfirmDialog(null, pane, "Please select the attribute for name", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
                    if (result != JOptionPane.OK_OPTION) return;

                    String name = selector.getSelectedItem().toString();

                    for (Node node : selectedNodes)
                        node.setName(node.getAttribute(name));

                    callback.updateVisualizationAfterChanges(Sets.newHashSet(NetworkElementType.NODE));
                    callback.addNetPlanChange();
                } catch (Throwable ex)
                {
                    ErrorHandling.showErrorDialog(ex.getMessage(), "Error retrieving name from attribute");
                }
            });

            options.add(nameFromAttribute);
        }

        return options;
    }

    @Override
    protected List<JComponent> getForcedOptions(ElementSelection selection)
    {
        List<JComponent> options = new LinkedList<>();

        final int numRows = model.getRowCount();
        if (numRows > 1)
        {
            JMenuItem showAllNodes = new JMenuItem("Show selected");
            showAllNodes.addActionListener(e ->
            {
                for (int row = 0; row < numRows; row++)
                    if (model.getValueAt(row, COLUMN_SHOWHIDE) != null)
                    {
                        if (model.getValueAt(row, 0) instanceof LastRowAggregatedValue) continue;
                        final long nodeId = (Long) model.getValueAt(row, 0);
                        final Node node = callback.getDesign().getNodeFromId(nodeId);
                        callback.getVisualizationState().showOnCanvas(node);
                    }
                callback.updateVisualizationAfterChanges(Sets.newHashSet(NetworkElementType.NODE));
                callback.addNetPlanChange();
            });

            options.add(showAllNodes);

            JMenuItem hideAllNodes = new JMenuItem("Hide all nodes");
            hideAllNodes.addActionListener(e ->
            {
                for (int row = 0; row < numRows; row++)
                    if (model.getValueAt(row, COLUMN_SHOWHIDE) != null)
                    {
                        if (model.getValueAt(row, 0) instanceof LastRowAggregatedValue) continue;
                        final long nodeId = (Long) model.getValueAt(row, 0);
                        final Node node = callback.getDesign().getNodeFromId(nodeId);
                        callback.getVisualizationState().hideOnCanvas(node);
                    }
                callback.updateVisualizationAfterChanges(Sets.newHashSet(NetworkElementType.NODE));
                callback.addNetPlanChange();
            });

            options.add(hideAllNodes);
        }

        return options;
    }

    private List<Node> getVisibleElementsInTable()
    {
        final ITableRowFilter rf = callback.getVisualizationState().getTableRowFilter();
        final NetworkLayer layer = callback.getDesign().getNetworkLayerDefault();
        return rf == null ? callback.getDesign().getNodes() : rf.getVisibleNodes(layer);
    }
}
