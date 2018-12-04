import re
import numpy as np
import matplotlib.pyplot as plt

input_file = '/home/mlux/projects/wipo2018/train/weights32.txt'
state = 0
count_images = 0
count_classes = 0
current_image = ''
current_classes = ''


def output(data, file = None):
    """
    outputs either to file or to std out
    :param data: the string to write
    :param file: the file to write into
    """
    if file is None:
        print(data)
    else:
        file.write(data + "\n")


if __name__ == "__main__":
    file_out = open('test.xml', 'w')
    # parse data into an XML file understood by Solr ..
    with open(input_file) as f:
        for line in f:
            l = line.strip()
            if len(l) == 0:
                if state == 2 and count_classes != 32:
                    print(current_image + ': ' + str(count_classes))
                if current_image is not '':
                    output("<doc>", file_out)
                    output("\t<field name=\"id\">" + current_image + "</field>", file_out)
                    output("\t<field name=\"classes_ws\">" + current_classes.strip() + "</field>", file_out)
                    output("</doc>", file_out)
                # reset everything
                state = 0
                count_classes = 0
                current_image = ''
                current_classes = ''
            if (l.lower().endswith('.png') or l.lower().endswith('.jpg')) and state == 0:
                current_image = l[2:]
                count_images += 1
                state = 1
            if state == 1 and l.lower() == 'found classes:':
                state = 2
            if state == 2 and re.search("\d\d\.\d\d\.\d\d", l) is not None:
                count_classes += 1
                class_id = re.search("\d\d\.\d\d\.\d\d", l).group(0)
                weight = float(re.search("\d+\.\d\d$", l).group(0))
                for i in range(0, round(weight)):
                    current_classes += class_id + " "

    print("{} files".format(count_images))
