package com.bbn.serif.util;

import com.bbn.bue.common.parameters.Parameters;
import com.bbn.serif.io.SerifXMLLoader;
import com.bbn.serif.io.SerifXMLWriter;
import com.bbn.serif.theories.DocTheory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbn.bue.common.files.FileUtils;

import java.io.File;
import java.util.List;


public class TestNonSynNodeMentionRoundTripper {


    public static void main(String[] args) throws Exception{
        System.out.println("Hello world!");

        final SerifXMLLoader loader = SerifXMLLoader.builderWithDynamicTypes().allowSloppyOffsets().build();
        final SerifXMLWriter writer = SerifXMLWriter.create();

        String inputSerifxmlList = "/d4m/ears/expts/48158.071921.all_cn_commoncrawl_vol2.v1/expts/time-modules/pyserif_nlp/cn/serif.list";
        String outputSerifxmlDirectory = "/nfs/raid66/u11/users/brozonoy-ad/jserif_output_dir";

        for (final File inputFile : FileUtils.loadFileList(new File(inputSerifxmlList))) {
            final DocTheory dt = loader.loadFrom(inputFile);
            File outputSerifXMLFile = new File(outputSerifxmlDirectory, dt.docid() + ".xml");
            writer.saveTo(dt, outputSerifXMLFile);
        }

    }

}
