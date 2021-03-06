package com.lizo.busflow.config;

import com.lizo.busflow.pattern.PatternType;
import com.lizo.busflow.routing.impl.DefaultRouting;
import com.lizo.busflow.routing.impl.SimpleRoutingCondition;
import com.lizo.busflow.station.BusHandlerMethod;
import com.lizo.busflow.station.DefaultStationRoutingWrap;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

import static org.springframework.beans.factory.xml.BeanDefinitionParserDelegate.ID_ATTRIBUTE;

/**
 * 处理<bf:stop>标签
 * 对于ref的Station，使用{@link StationRoutingWrap}进行包装
 * Created by lizhou on 2017/3/14/014.
 */
public class BusinessFlowStopDefinitionParser implements BeanDefinitionParser {
    public BeanDefinition parse(Element element, ParserContext parserContext) {
        String id = element.getAttribute(ID_ATTRIBUTE);
        String ref = element.getAttribute("ref");
        String method = element.getAttribute("method");

        RootBeanDefinition nodeWrapDefinition = new RootBeanDefinition();
        nodeWrapDefinition.setBeanClass(DefaultStationRoutingWrap.class);

        //解析ref属性，因为ref引用的也是一个StationRoutingWrap,可能在这里还未注册
        //因此使用RuntimeBeanReference
        RuntimeBeanReference stationRef = new RuntimeBeanReference(ref);

        //解析子标签，子标签为一个list,
        // 这里不能直接用List<BeanDefinition>，而要用ManagedList，运行时去解析BeanDefinition
        ManagedList routingConditions = new ManagedList();
        int length = element.getChildNodes().getLength();
        for (int i = 0; i < length; i++) {
            org.w3c.dom.Node node = element.getChildNodes().item(i);
            if (node.getNodeType() == node.ELEMENT_NODE) {
                Element e = (Element) node;
                if ("bf:routing".equals(e.getTagName())) {
                    routingConditions.add(dealHeadRouting(e));
                }
            }
        }


        RootBeanDefinition routing = new RootBeanDefinition();
        routing.setBeanClass(DefaultRouting.class);
        routing.getPropertyValues().add("routingConditions", routingConditions);

        nodeWrapDefinition.getPropertyValues().add("routing", routing);

        //handleMethod
        RootBeanDefinition handleMethodBeanDefinition = new RootBeanDefinition();
        handleMethodBeanDefinition.setBeanClass(BusHandlerMethod.class);
        ConstructorArgumentValues constructorArgumentValues = new ConstructorArgumentValues();
        constructorArgumentValues.addIndexedArgumentValue(0, stationRef);
        constructorArgumentValues.addIndexedArgumentValue(1, method);
        handleMethodBeanDefinition.setConstructorArgumentValues(constructorArgumentValues);
        nodeWrapDefinition.getPropertyValues().add("handlerMethod", handleMethodBeanDefinition);

        BeanDefinitionHolder holder = new BeanDefinitionHolder(nodeWrapDefinition, id);
        BeanDefinitionReaderUtils.registerBeanDefinition(holder, parserContext.getRegistry());
        return nodeWrapDefinition;
    }

    private BeanDefinition dealHeadRouting(Element e) {
        String value = e.getAttribute("value");
        String to = e.getAttribute("to");
        String patten = e.getAttribute("patten");


        RootBeanDefinition rootBeanDefinition = new RootBeanDefinition();
        rootBeanDefinition.setBeanClass(SimpleRoutingCondition.class);
        rootBeanDefinition.getPropertyValues().add("condition", value);
        rootBeanDefinition.getPropertyValues().add("pattern", PatternType.valueOf(patten));
        rootBeanDefinition.getPropertyValues().add("stationRoutingWrap", new RuntimeBeanReference(to));

        return rootBeanDefinition;
    }


}
