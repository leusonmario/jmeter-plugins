package kg.apc.jmeter.vizualizers;

import kg.apc.jmeter.charting.DateTimeRenderer;
import java.util.HashSet;
import java.util.Iterator;
import javax.swing.ImageIcon;
import kg.apc.jmeter.charting.AbstractGraphRow;
import org.apache.jmeter.reporters.ResultCollector;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.testelement.property.CollectionProperty;
import org.apache.jmeter.testelement.property.JMeterProperty;
import org.apache.jmeter.testelement.property.NullProperty;
import org.apache.jmeter.testelement.property.PropertyIterator;

public class CompositeGraphGui extends AbstractOverTimeVisualizer
{
    private JCompositeRowsSelectorPanel compositeRowsSelectorPanel;
    public CompositeModel compositeModel;
    private long lastUpdate = 0;
    private static String CONFIG_PROPERTY = "COMPOSITE_CFG";

    public CompositeGraphGui()
    {
        graphPanel.getGraphObject().setDisplayPrecision(false);
        compositeModel = new CompositeModel();
        ImageIcon rowsIcon = new ImageIcon(CompositeGraphGui.class.getResource("checks.png"));
        graphPanel.remove(1);
        compositeRowsSelectorPanel = new JCompositeRowsSelectorPanel(compositeModel, this);
        compositeModel.setNotifier((CompositeNotifierInterface)compositeRowsSelectorPanel);
        graphPanel.insertTab("Graphs", rowsIcon, compositeRowsSelectorPanel, "Select graphs/rows to display", 1);

        graphPanel.getGraphObject().setxAxisLabelRenderer(new DateTimeRenderer("HH:mm:ss"));
        graphPanel.getGraphObject().setReSetColors(true);

        graphPanel.getGraphObject().setxAxisLabel("Elapsed time");
        graphPanel.getGraphObject().setyAxisLabel("Scaled values");

        graphPanel.getGraphObject().setExpendRows(true);
        
        CompositeResultCollector compositeResultCollector = new CompositeResultCollector();
        compositeResultCollector.setCompositeModel(compositeModel);
        setModel(compositeResultCollector);

        getFilePanel().setVisible(false);
    }

    @Override
    protected JSettingsPanel getSettingsPanel()
    {
        return new JSettingsPanel(this, false, true, false, true, true, false, false, false, true);
    }

    @Override
    public String getLabelResource()
    {
        return this.getClass().getSimpleName();
    }

    @Override
    public String getStaticLabel()
    {
        return "Composite Graph";
    }

   @Override
   public TestElement createTestElement()
   {
        ResultCollector modelNew = getModel();
        if (modelNew == null) {
            modelNew = new CompositeResultCollector();
            ((CompositeResultCollector)modelNew).setCompositeModel(compositeModel);
            setModel(modelNew);
        }
        modifyTestElement(modelNew);
        return modelNew;
   }

    @Override
    public void configure(TestElement te)
    {
        //log.info("Configure");
        super.configure(te);
        ((CompositeResultCollector)te).setCompositeModel(compositeModel);

        JMeterProperty data = te.getProperty(CONFIG_PROPERTY);

        if (!(data instanceof NullProperty))
        {
            setConfig((CollectionProperty)data);
        }
    }

    @Override
    public void modifyTestElement(TestElement c)
    {
        super.modifyTestElement(c);
        c.setProperty(getConfig());
    }

    private CollectionProperty getConfig()
    {
        CollectionProperty ret = new CollectionProperty();
        CollectionProperty testplans = new CollectionProperty();
        CollectionProperty rows = new CollectionProperty();

        ret.setName(CONFIG_PROPERTY);
        Iterator<String[]> iter = compositeRowsSelectorPanel.getItems();

        while(iter.hasNext())
        {
            String[] item = iter.next();
            testplans.addItem(item[0]);
            rows.addItem(item[1]);
        }

        ret.addItem(testplans);
        ret.addItem(rows);
        return ret;
    }

    private void setConfig(CollectionProperty properties)
    {
        PropertyIterator iter = properties.iterator();

        CollectionProperty testplans = (CollectionProperty) iter.next();
        CollectionProperty rows = (CollectionProperty) iter.next();

        if(rows.size() > 0)
        {
            PropertyIterator iterTestplans = testplans.iterator();
            PropertyIterator iterRows = rows.iterator();

            while (iterTestplans.hasNext() && iterRows.hasNext())
            {
                String testplan = iterTestplans.next().getStringValue();
                String row = iterRows.next().getStringValue();
                compositeRowsSelectorPanel.addItemsToComposite(testplan, row);
            }
            
        }
    }

    @Override
    public void updateGui()
    {
        Iterator<String[]> iter = compositeRowsSelectorPanel.getItems();
        HashSet<String> validRows = new HashSet<String>();
        while (iter.hasNext())
        {
            String[] item = iter.next();
            AbstractGraphRow row = compositeModel.getRow(item[0], item[1]);
            if (row != null)
            {
                String rowName = item[0] + ">" + item[1];
                validRows.add(rowName);
                if (!model.containsKey(rowName))
                {
                    model.put(rowName, row);
                }
            }
        }
        //remove invalid rows
        Iterator<String> iterModelRows = model.keySet().iterator();
        while(iterModelRows.hasNext())
        {
            String rowName = iterModelRows.next();
            if(!validRows.contains(rowName))
            {
                iterModelRows.remove();
            }
        }
        super.updateGui();
    }

    @Override
    public void add(SampleResult sr)
    {
        super.add(sr);
        long time = System.currentTimeMillis();

        if (time > lastUpdate + 1000)
        {
            lastUpdate = time;
            updateGui();
        }
    }
}
