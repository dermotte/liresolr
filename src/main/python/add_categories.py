import sys

from keras.applications.resnet50 import ResNet50
from keras.preprocessing import image
from keras.applications.resnet50 import preprocess_input, decode_predictions
import numpy as np
import math
import urllib
import xml.etree.ElementTree as ET
import os.path
import traceback

allFeatures = np.array([])

# install with pip: tensorflow, keras, h5py

# reading paths of images from a file
# using keras to predict the topmost classes and writing them to
# the file in a format usable for indexing in Lucene in the field categories_ws
# the probability of detection gives the tem frequency [0.9,1) -> ten times the term.
# Mathias Lux, 2017-02-02

# if you put flickrdownloader.jar in the same directory and the XML file is not found,
# the jar will be called. If you run it with the "auto" argument and flickrdownloader.jar
# in the same directory it will download 100 images and assign categories as well as name
# the resulting files in ascending numbers.

# don't forget to install pillow / PIL for opening the images ...

if len(sys.argv) < 2:
    print('too few arguments, give a list of images in a file as argument')
    sys.exit()


def predictClasses(myModel, img):
    # img = image.load_img(img_path, target_size=(224, 224))
    x = image.img_to_array(img)
    x = np.expand_dims(x, axis=0)
    x = preprocess_input(x)

    preds = myModel.predict(x)
    # decode the results into a list of tuples (class, description, probability)
    # (one such list for each sample in the batch)
    predictions = decode_predictions(preds, top=5)[0]
    tmpString = ''
    for p in predictions:
        for i in range(0, int(math.ceil(p[2] * 10))):
            tmpString += p[1] + ' '  # putting together the predictions for Solr.
    return tmpString


def extractFeatures(myModel, img):
    # img = image.load_img(img_path, target_size=(224, 224))
    x = image.img_to_array(img)
    x = np.expand_dims(x, axis=0)
    x = preprocess_input(x)

    features = myModel.predict(x)[0,]
    features = features / np.max(features)  # max norm
    # features = features / np.linalg.norm(features, ord=2)  # L2 norm
    features = np.ndarray.astype(features*32767*2-32768, dtype='int16')  # stretching it over the whole range of short ...
    global allFeatures
    allFeatures = np.append(allFeatures, features)
    dataString = ''
    for f in features:
        dataString += str(f) + ';'
    cmd = 'java -cp flickrdownloader.jar net.semanticmetadata.lire.solr.tools.DataConversion -t short -d \"x' + dataString + "\""
    return os.popen(cmd).read()


# loading the model
model_categories = ResNet50(weights='imagenet')
model_features = ResNet50(weights='imagenet', include_top=False, pooling='avg')

# checking for file names ...
fileNumber = 1000
if (sys.argv[1] == "auto"):
    while os.path.isfile("flickrphotos-" + str(fileNumber) + ".xml"):
        fileNumber += 1
    sys.argv[1] = "flickrphotos-" + str(fileNumber) + ".xml"  # set new file name ...

if not (os.path.isfile(sys.argv[1])):
    os.system("java -jar flickrdownloader.jar -s -n 50 -o " + sys.argv[1])

tree = ET.parse(sys.argv[1])
root = tree.getroot()
for doc in root:
    imagefile = ""
    imageurl = ""
    for field in doc:
        if (field.attrib['name'] == 'localimagefile'):
            imagefile = field.text
            imagefileField = field
        if (field.attrib['name'] == 'imgurl'):
            imageurl = field.text

    try:
        if (imagefile == ""):
            downloaded = urllib.urlretrieve(field.text)
            imagefile = downloaded[0]
        else:
            doc.remove(imagefileField)

        img = image.load_img(imagefile, target_size=(224, 224))
        # ### creating the categories based on ResNet50
        e = ET.Element('field', {'name': 'categories_ws'})
        # e.text = "cat cat dog"
        e.text = predictClasses(model_categories, img)
        doc.append(e)
        print(imagefile + ': ' + e.text)

        # ### creating the feature vectors based on ResNet50,
        e2 = ET.Element('field', {'name': 'sf_hi'})  # storing in a short feature right now.
        e3 = ET.Element('field', {'name': 'sf_ha'})  # storing in a short feature right now.
        # e2 = ET.Element('field', {'name': 'df_hi'})  # storing in a double feature right now.
        features = extractFeatures(model_features, img).split('|')
        e2.text = features[0]
        e3.text = features[1]
        doc.append(e2)
        doc.append(e3)
        print(imagefile + ': ' + e2.text)

        # ### removing the temp file, clean up.
        # os.remove(imagefile)
    except:
        print('error with ' + imagefile)  # general description
        traceback.print_stack()  # full error description?
        pass
tree.write(os.path.splitext(sys.argv[1])[0] + "_cat.xml")
print(allFeatures)


# reading images from a file
# lines = [line.rstrip('\n') for line in open(sys.argv[1])]
# for imagePath in lines :
#     print imagePath + ': ' + myPredict(model, imagePath)
