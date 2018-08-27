For the Huffman Coding assignment, I wrote a program that compresses and decompresses any file using the optimal per-character 
compression method of Huffman Coding.

I completed the class HuffProcessor that takes in an input stream and writes the compressed/decompressed file to a given output stream 
using Huffman Coding. I used a GUI, HuffMain, to verify that my compress and decompress methods in HuffProcessor were correct 
I also wrote JUnit tests to verify specific steps of my compression algorithm were correct. 

Writing this program gave me deep insight on how something that I use everyday, compressed files like jpg, png, etc, work. The steps in a 
compression algorithm include creating a tree for both compression and decompression and analyzing the files with a BitInputStream or a
BitOutputStream. 

To better understand the performance of my code, I used a large directory of uncompressed files and ran them through a GUI processor and 
received data on compression rate. The lower the compression rate the better. I benchmarked my code in this way using rtf files I created 
myself as well as png and jpg files that were given. I found that overall, text files compress more than image files do, which makes sense
since the Huffman trees are generally larger and therefore less "compressable" in image files than text files. 
