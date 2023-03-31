package com.peopleapp.service;

import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.IMongodConfig;
import de.flapdoodle.embed.mongo.config.MongodConfigBuilder;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.runtime.Network;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class BaseTest {

    public static MongodExecutable mongodExecutable;

    @BeforeClass
    public static void embeddedMongoSetUp() throws Exception {
        MongodStarter starter = MongodStarter.getDefaultInstance();
        String bindIp = "localhost";
        int port = 27019;
        IMongodConfig mongodConfig = new MongodConfigBuilder()
                .version(Version.Main.PRODUCTION)
                .net(new Net(bindIp, port, Network.localhostIsIPv6()))
                .build();
        mongodExecutable = null;
        try {
            mongodExecutable = starter.prepare(mongodConfig);
            mongodExecutable.start();
        } catch (Exception e) {
            // log exception here
            if (mongodExecutable != null)
                mongodExecutable.stop();
        }
    }


    @AfterClass
    public static void teardownMongo() throws Exception {
        if (mongodExecutable != null)
            mongodExecutable.stop();
    }

    @Test
    public void baseTest() {

        /* Using dummy assertion statement to satisfy @test annotation */
        Assert.assertTrue("This is a base Test class for all other Test classes",true);
    }
}
