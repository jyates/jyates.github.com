# Converting Avro Generic Record to Spark Row

See RowConverter.

Step 1
Take the schema and convert to a struct. Done without the SchemaConversion to ensure that we retain the order of the fields in the struct. This is important.

Step 2
Convert to record to row. Initially used Record -> Json -> Map -> Row, for learning how everything work, but easier to just go Record -> Row once you understand what is happening. We follow the same procedure when stepping through the schema as when creating the struct. This is necessary b/c when Spark makes the conversion in `CatalystTypeConverters.scala` it maps the converters based on index.

Biggest issue is that arrays are just managed as arrays while records need to be converted into Row, even within the parent row. Once you have that, its merely a matter of walking the hierarchy to get the resulting struct.

Caveat for java: recursive calls need to return Object[], not Object (even if it is actually an Object[]) when using the varargs constructor for the GenericRow/org.apache.spark.sql.RowFactory#create. More of a java thing, but easy to get tripped up on, leading to incorrectly formatted rows (e.g. array of array, rather than just the array of values).
