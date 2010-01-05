/*
 * Copyright 2009 Red Hat, Inc.
 * Red Hat licenses this file to you under the Apache License, version
 * 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *    http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */
package org.hornetq.tests.integration.client;

import junit.framework.Assert;

import org.hornetq.api.core.client.ClientProducer;
import org.hornetq.api.core.client.ClientSession;
import org.hornetq.api.core.client.ClientSessionFactory;
import org.hornetq.api.core.client.ClientSessionFactoryImpl;
import org.hornetq.api.core.config.Configuration;
import org.hornetq.api.core.config.ConfigurationImpl;
import org.hornetq.api.core.config.TransportConfiguration;
import org.hornetq.api.core.exception.HornetQException;
import org.hornetq.api.core.server.HornetQServers;
import org.hornetq.core.remoting.impl.invm.InVMAcceptorFactory;
import org.hornetq.core.remoting.impl.invm.InVMConnectorFactory;
import org.hornetq.core.server.HornetQServer;
import org.hornetq.tests.util.RandomUtil;
import org.hornetq.tests.util.ServiceTestBase;
import org.hornetq.tests.util.UnitTestCase;

/**
 * 
 * @author <a href="mailto:jmesnil@redhat.com">Jeff Mesnil</a>
 */
public class ProducerCloseTest extends ServiceTestBase
{

   // Constants -----------------------------------------------------

   // Attributes ----------------------------------------------------

   private HornetQServer server;

   private ClientSession session;

   // Static --------------------------------------------------------

   // Constructors --------------------------------------------------

   // Public --------------------------------------------------------

   public void testCanNotUseAClosedProducer() throws Exception
   {
      final ClientProducer producer = session.createProducer(RandomUtil.randomSimpleString());

      Assert.assertFalse(producer.isClosed());

      producer.close();

      Assert.assertTrue(producer.isClosed());

      UnitTestCase.expectHornetQException(HornetQException.OBJECT_CLOSED, new HornetQAction()
      {
         public void run() throws HornetQException
         {
            producer.send(session.createMessage(false));
         }
      });
   }

   // Package protected ---------------------------------------------

   // Protected -----------------------------------------------------

   @Override
   protected void setUp() throws Exception
   {
      super.setUp();
      Configuration config = new ConfigurationImpl();
      config.getAcceptorConfigurations().add(new TransportConfiguration(InVMAcceptorFactory.class.getCanonicalName()));
      config.setSecurityEnabled(false);
      server = HornetQServers.newHornetQServer(config, false);
      server.start();

      sf = new ClientSessionFactoryImpl(new TransportConfiguration(InVMConnectorFactory.class.getName()));
      session = sf.createSession(false, true, true);
   }

   private ClientSessionFactory sf;

   @Override
   protected void tearDown() throws Exception
   {
      session.close();

      sf.close();

      server.stop();

      server = null;

      session = null;

      super.tearDown();
   }

   // Private -------------------------------------------------------

   // Inner classes -------------------------------------------------

}
