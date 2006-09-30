/**
 * JBoss, Home of Professional Open Source
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jboss.example.jms.distributedtopic;

import javax.jms.MessageListener;
import javax.jms.Message;

/**
 * @author <a href="mailto:ovidiu@jboss.org">Ovidiu Feodorov</a>
 * @author <a href="mailto:clebert.suconic@jboss.org">Clebert Suconic</a>
 * @version <tt>$Revision: 536 $</tt>

 * $Id: ExampleListener.java 536 2005-12-22 00:28:39 -0600 (Thu, 22 Dec 2005) ovidiu $
 */
public class ExampleListener implements MessageListener
{
   // Constants -----------------------------------------------------

   // Static --------------------------------------------------------

   // Attributes ----------------------------------------------------

   private String name;
   private Message message;

   // Constructors --------------------------------------------------

   public ExampleListener(String name)
   {
      this.name = name;
   }

   // MessageListener implementation --------------------------------

   public synchronized void onMessage(Message message)
   {
      this.message = message;
      notifyAll();
   }

   // Public --------------------------------------------------------

   public synchronized Message getMessage()
   {
      return message;
   }

   public synchronized void waitForMessage()
   {
      if (message != null)
      {
         return;
      }

      try
      {
         wait(5000);
      }
      catch(InterruptedException e)
      {
         // OK
      }
   }

   public String getName()
   {
      return name;
   }

   // Package protected ---------------------------------------------

   // Protected -----------------------------------------------------

   // Private -------------------------------------------------------

   // Inner classes -------------------------------------------------

}
