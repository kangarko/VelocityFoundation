<p align="center">
  <small><i>Learn Java, code Minecraft plugins and launch a unique network from the ground up in 20 days (without experience):</i></small>
  <a href="https://mineacademy.org/project-orion?st=github&sc=velocityfoundation&utm_source=github&utm_medium=overview&utm_campaign=velocityfoundation">
    <img src="https://i.imgur.com/SVHA9Kf.png" />
  </a>
</p>

[![](https://jitpack.io/v/kangarko/VelocityFoundation.svg)](https://jitpack.io/#kangarko/VelocityFoundation)

VelocityFoundation is a library for fast development of Velocity plugins.

## Why use VelocityFoundation?

* Syntax and classes similar to Bukkit/BungeeCord to help you transition over to Velocity.
* Focus on simplicity to avoid over-the-top abstractions and complicated names.
* A simpler command system.
* A ton of time-saving methods useful for people tired of dealing with abstract/complicated APIs (i.e. Common#tell(Player, String for simple player messaging without the need to use Adventure each time)

## Usage

1. Include [VelocityFoundation]([url](https://jitpack.io/#kangarko/VelocityFoundation)) to your Maven/Gradle dependency from our JitPack.
```xml
<repositories>
  <repository>
    <id>jitpack.io</id>
    <url>https://jitpack.io</url>
  </repository>
</repositories>
```
```xml
<dependency>
    <groupId>com.github.kangarko</groupId>
    <artifactId>VelocityFoundation</artifactId>
    <version>REPLACE_WITH_VERSION</version>
</dependency>
 ```
2. Include [velocity-api]([url](https://docs.papermc.io/velocity/dev/creating-your-first-plugin#setting-up-the-dependency)) and [lombok]([url](https://mvnrepository.com/artifact/org.projectlombok/lombok)) to your dependencies.

 ```xml
<dependency>
	<groupId>com.velocitypowered</groupId>
	<artifactId>velocity-api</artifactId>
	<version>3.3.0-SNAPSHOT</version>
	<scope>provided</scope>
</dependency>
<dependency>
	<groupId>org.projectlombok</groupId>
	<artifactId>lombok</artifactId>
	<version>1.18.32</version>
	<scope>provided</scope>
</dependency>
 ```

3. Relocate VelocityControl and its library nashorn. Here is a snippet for Maven to place inside <plugins> section. You need to change the shadedPattern to match your own package name.

```xml
<plugin>
  <groupId>org.apache.maven.plugins</groupId>
  <artifactId>maven-shade-plugin</artifactId>
  <version>3.5.1</version>
  <executions>
    <execution>
      <phase>package</phase>
      <goals>
        <goal>shade</goal>
      </goals>
    </execution>
  </executions>
  <configuration>
    <createDependencyReducedPom>false</createDependencyReducedPom>
    <relocations>
      <relocation>
        <pattern>org.mineacademy.vfo</pattern>
        <shadedPattern>org.mineacademy.velocitycontrol.lib</shadedPattern>
      </relocation>
      <relocation>
        <pattern>org.objectweb.asm</pattern>
        <shadedPattern>org.mineacademy.velocitycontrol.nashorn.asm</shadedPattern>
      </relocation>
    </relocations>
  </configuration>
</plugin>
```

4. Make your main plugin class override SimplePlugin and implement the necessary constructor. See the example below for an empty main plugin's class (make sure to change @Plugin info to match your own info).

```java
package org.mineacademy.chatsync;

import java.nio.file.Path;

import org.mineacademy.velocitycontrol.listener.ChatListener;
import org.mineacademy.vfo.command.ReloadCommand;
import org.mineacademy.vfo.plugin.SimplePlugin;
import org.slf4j.Logger;

import com.google.inject.Inject;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;

@Plugin(id = "chatsync", name = "ChatSync", version = "1.0.0", authors = { "kangarko" })
public final class ChatSyncPlugin extends SimplePlugin {

	@Inject
	public ChatSyncPlugin(final ProxyServer proxyServer, Logger logger, final @DataDirectory Path dataDirectory) {
		super(proxyServer, logger, dataDirectory);
	}

	// This method is called on server start
	@Override
	protected void onPluginStart() {
	}

	// This method is called on server stop
	@Override
	protected void onPluginStop() {
	}

	// This method is called on /chatreload and on the plugin startup, place your event, command and runnable registration here
	@Override
	protected void onReloadablesStart() {
		//this.registerEvents(new ChatListener());
		//this.registerCommand(new ReloadCommand("chatreload", "chatcontrol.command.reload"));
	}
}

```

## Licencing Information

© MineAcademy.org

Tl;dl: You can do whatever you want as long as you don't claim this library as your own or don't sell or resell parts of it. If you are not a paying student of MineAcademy, you MUST place a link to this GitHub page in your sales pages (for example on Overview pages on Spigot) if your paid software is using Foundation.

1) **If you are a paying student of MineAcademy.org** then you can use, modify and
reproduce this library both commercially and non-commercially for yourself, your team
or network without attribution.

4) **If you are not a paying student of MineAcademy.org** then you may
use this library as stated above however you must clearly attribute that you
are using this library in your software by linking to this GitHub page.

In both of the above cases, do not sell or claim any part of this library as your own.

No guarantee - this software is provided AS IS, without any guarantee on its
functionality. We made our best efforts to make VelocityFoundation an enterprise-level
solution for anyone looking to accelerate his coding however we are not
taking any responsibility for the success or failure you achieve using it.
