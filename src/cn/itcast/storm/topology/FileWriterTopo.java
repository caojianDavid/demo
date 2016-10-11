package cn.itcast.storm.topology;

import backtype.storm.Config;
import backtype.storm.StormSubmitter;
import backtype.storm.topology.TopologyBuilder;
import backtype.storm.tuple.Fields;
import cn.itcast.storm.bolt.FieldsGroupingBolt;
import cn.itcast.storm.bolt.WordCounter;
import cn.itcast.storm.bolt.WriterBolt;
import cn.itcast.storm.spout.MetaSpout;
import cn.itcast.storm.spout.StringScheme;
import cn.itcast.storm.utils.PropertyUtil;

import com.taobao.metamorphosis.client.MetaClientConfig;
import com.taobao.metamorphosis.client.consumer.ConsumerConfig;
import com.taobao.metamorphosis.utils.ZkUtils.ZKConfig;

public class FileWriterTopo {

	/**
	 * 数据写入文本的Topo
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		
		if (args.length != 2) {
			System.err.println("Usage: topic group");
			System.exit(2);
		}
		String topic = args[0];
		String group = args[1];
		
		// 设置ZK配置信息
		final ZKConfig zkConfig = new ZKConfig();
		zkConfig.zkConnect = PropertyUtil.getProperty("zkConnect");
		zkConfig.zkSessionTimeoutMs = Integer.parseInt(PropertyUtil.getProperty("zkSessionTimeoutMs"));
		zkConfig.zkConnectionTimeoutMs = Integer.parseInt(PropertyUtil.getProperty("zkConnectionTimeoutMs"));
		zkConfig.zkSyncTimeMs = Integer.parseInt(PropertyUtil.getProperty("zkSyncTimeMs"));

		// 设置MetaQ
		final MetaClientConfig metaClientConfig = new MetaClientConfig();
		metaClientConfig.setZkConfig(zkConfig);
		ConsumerConfig consumerConfig = new ConsumerConfig(group);
		
		TopologyBuilder builder = new TopologyBuilder();
		Config conf = new Config();
		conf.setNumWorkers(2);
		builder.setSpout("meta-spout", new MetaSpout(metaClientConfig, topic, consumerConfig, new StringScheme()));
		builder.setBolt("field-grouping-bolt", new FieldsGroupingBolt(), 2).shuffleGrouping("meta-spout");
		builder.setBolt("file-writer-bolt", new WriterBolt(), 4).fieldsGrouping("field-grouping-bolt", new Fields("partition"));
		StormSubmitter.submitTopology("fileWriter", conf, builder.createTopology());
		

	}

}
