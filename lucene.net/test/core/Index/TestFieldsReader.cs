/* 
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

using System;
using System.Collections.Generic;
using Lucene.Net.Support;
using NUnit.Framework;

using WhitespaceAnalyzer = Lucene.Net.Analysis.WhitespaceAnalyzer;
using Lucene.Net.Documents;
using AlreadyClosedException = Lucene.Net.Store.AlreadyClosedException;
using BufferedIndexInput = Lucene.Net.Store.BufferedIndexInput;
using Directory = Lucene.Net.Store.Directory;
using FSDirectory = Lucene.Net.Store.FSDirectory;
using IndexInput = Lucene.Net.Store.IndexInput;
using IndexOutput = Lucene.Net.Store.IndexOutput;
using RAMDirectory = Lucene.Net.Store.RAMDirectory;
using LuceneTestCase = Lucene.Net.Util.LuceneTestCase;
using _TestUtil = Lucene.Net.Util._TestUtil;

namespace Lucene.Net.Index
{

    [TestFixture]
    public class TestFieldsReader : LuceneTestCase
	{
		[Serializable]
		private class AnonymousClassFieldSelector : FieldSelector
		{
			public AnonymousClassFieldSelector(TestFieldsReader enclosingInstance)
			{
				InitBlock(enclosingInstance);
			}
			private void  InitBlock(TestFieldsReader enclosingInstance)
			{
				this.enclosingInstance = enclosingInstance;
			}
			private TestFieldsReader enclosingInstance;
			public TestFieldsReader Enclosing_Instance
			{
				get
				{
					return enclosingInstance;
				}
				
			}
			public virtual FieldSelectorResult Accept(System.String fieldName)
			{
				if (fieldName.Equals(DocHelper.TEXT_FIELD_1_KEY) ||  fieldName.Equals(DocHelper.LAZY_FIELD_BINARY_KEY))
					return FieldSelectorResult.SIZE;
				else if (fieldName.Equals(DocHelper.TEXT_FIELD_3_KEY))
					return FieldSelectorResult.LOAD;
				else
					return FieldSelectorResult.NO_LOAD;
			}
		}
		private RAMDirectory dir = new RAMDirectory();
		private Document testDoc = new Document();
		private FieldInfos fieldInfos = null;
		
		private const System.String TEST_SEGMENT_NAME = "_0";
		
		public TestFieldsReader(System.String s):base(s)
		{
		}

        public TestFieldsReader() : base("")
        {
        }
		
		[SetUp]
		public override void  SetUp()
		{
			base.SetUp();
			fieldInfos = new FieldInfos();
			DocHelper.SetupDoc(testDoc);
			fieldInfos.Add(testDoc);
			IndexWriter writer = new IndexWriter(dir, new WhitespaceAnalyzer(), true, IndexWriter.MaxFieldLength.LIMITED);
			writer.UseCompoundFile = false;
			writer.AddDocument(testDoc);
			writer.Close();
		}

        [TearDown]
        public override void TearDown()
        {
            fieldInfos = null;
            testDoc = new Document();
            dir = new RAMDirectory();
        }
		
		[Test]
		public virtual void  Test()
		{
			Assert.IsTrue(dir != null);
			Assert.IsTrue(fieldInfos != null);
			FieldsReader reader = new FieldsReader(dir, TEST_SEGMENT_NAME, fieldInfos);
			Assert.IsTrue(reader != null);
			Assert.IsTrue(reader.Size() == 1);
			Document doc = reader.Doc(0, null);
			Assert.IsTrue(doc != null);
			Assert.IsTrue(doc.GetField(DocHelper.TEXT_FIELD_1_KEY) != null);
			
			IFieldable field = doc.GetField(DocHelper.TEXT_FIELD_2_KEY);
			Assert.IsTrue(field != null);
			Assert.IsTrue(field.IsTermVectorStored == true);
			
			Assert.IsTrue(field.IsStoreOffsetWithTermVector == true);
			Assert.IsTrue(field.IsStorePositionWithTermVector == true);
			Assert.IsTrue(field.OmitNorms == false);
			Assert.IsTrue(field.OmitTermFreqAndPositions == false);
			
			field = doc.GetField(DocHelper.TEXT_FIELD_3_KEY);
			Assert.IsTrue(field != null);
			Assert.IsTrue(field.IsTermVectorStored == false);
			Assert.IsTrue(field.IsStoreOffsetWithTermVector == false);
			Assert.IsTrue(field.IsStorePositionWithTermVector == false);
			Assert.IsTrue(field.OmitNorms == true);
			Assert.IsTrue(field.OmitTermFreqAndPositions == false);
			
			field = doc.GetField(DocHelper.NO_TF_KEY);
			Assert.IsTrue(field != null);
			Assert.IsTrue(field.IsTermVectorStored == false);
			Assert.IsTrue(field.IsStoreOffsetWithTermVector == false);
			Assert.IsTrue(field.IsStorePositionWithTermVector == false);
			Assert.IsTrue(field.OmitNorms == false);
			Assert.IsTrue(field.OmitTermFreqAndPositions == true);
			reader.Dispose();
		}
		
		
		[Test]
		public virtual void  TestLazyFields()
		{
			Assert.IsTrue(dir != null);
			Assert.IsTrue(fieldInfos != null);
			FieldsReader reader = new FieldsReader(dir, TEST_SEGMENT_NAME, fieldInfos);
			Assert.IsTrue(reader != null);
			Assert.IsTrue(reader.Size() == 1);
            ISet<string> loadFieldNames = Support.Compatibility.SetFactory.CreateHashSet<string>();
			loadFieldNames.Add(DocHelper.TEXT_FIELD_1_KEY);
			loadFieldNames.Add(DocHelper.TEXT_FIELD_UTF1_KEY);
            ISet<string> lazyFieldNames = Support.Compatibility.SetFactory.CreateHashSet<string>();
			//new String[]{DocHelper.LARGE_LAZY_FIELD_KEY, DocHelper.LAZY_FIELD_KEY, DocHelper.LAZY_FIELD_BINARY_KEY};
			lazyFieldNames.Add(DocHelper.LARGE_LAZY_FIELD_KEY);
			lazyFieldNames.Add(DocHelper.LAZY_FIELD_KEY);
			lazyFieldNames.Add(DocHelper.LAZY_FIELD_BINARY_KEY);
			lazyFieldNames.Add(DocHelper.TEXT_FIELD_UTF2_KEY);
			SetBasedFieldSelector fieldSelector = new SetBasedFieldSelector(loadFieldNames, lazyFieldNames);
			Document doc = reader.Doc(0, fieldSelector);
			Assert.IsTrue(doc != null, "doc is null and it shouldn't be");
			IFieldable field = doc.GetFieldable(DocHelper.LAZY_FIELD_KEY);
			Assert.IsTrue(field != null, "field is null and it shouldn't be");
			Assert.IsTrue(field.IsLazy, "field is not lazy and it should be");
			System.String value_Renamed = field.StringValue;
			Assert.IsTrue(value_Renamed != null, "value is null and it shouldn't be");
			Assert.IsTrue(value_Renamed.Equals(DocHelper.LAZY_FIELD_TEXT) == true, value_Renamed + " is not equal to " + DocHelper.LAZY_FIELD_TEXT);
			field = doc.GetFieldable(DocHelper.TEXT_FIELD_1_KEY);
			Assert.IsTrue(field != null, "field is null and it shouldn't be");
			Assert.IsTrue(field.IsLazy == false, "Field is lazy and it should not be");
			field = doc.GetFieldable(DocHelper.TEXT_FIELD_UTF1_KEY);
			Assert.IsTrue(field != null, "field is null and it shouldn't be");
			Assert.IsTrue(field.IsLazy == false, "Field is lazy and it should not be");
			Assert.IsTrue(field.StringValue.Equals(DocHelper.FIELD_UTF1_TEXT) == true, field.StringValue + " is not equal to " + DocHelper.FIELD_UTF1_TEXT);
			
			field = doc.GetFieldable(DocHelper.TEXT_FIELD_UTF2_KEY);
			Assert.IsTrue(field != null, "field is null and it shouldn't be");
			Assert.IsTrue(field.IsLazy == true, "Field is lazy and it should not be");
			Assert.IsTrue(field.StringValue.Equals(DocHelper.FIELD_UTF2_TEXT) == true, field.StringValue + " is not equal to " + DocHelper.FIELD_UTF2_TEXT);
			
			field = doc.GetFieldable(DocHelper.LAZY_FIELD_BINARY_KEY);
			Assert.IsTrue(field != null, "field is null and it shouldn't be");
			Assert.IsTrue(field.StringValue == null, "stringValue isn't null for lazy binary field");
			
			byte[] bytes = field.GetBinaryValue();
			Assert.IsTrue(bytes != null, "bytes is null and it shouldn't be");
			Assert.IsTrue(DocHelper.LAZY_FIELD_BINARY_BYTES.Length == bytes.Length, "");
			for (int i = 0; i < bytes.Length; i++)
			{
				Assert.IsTrue(bytes[i] == DocHelper.LAZY_FIELD_BINARY_BYTES[i], "byte[" + i + "] is mismatched");
			}
		}
		
		[Test]
		public virtual void  TestLazyFieldsAfterClose()
		{
			Assert.IsTrue(dir != null);
			Assert.IsTrue(fieldInfos != null);
			FieldsReader reader = new FieldsReader(dir, TEST_SEGMENT_NAME, fieldInfos);
			Assert.IsTrue(reader != null);
			Assert.IsTrue(reader.Size() == 1);
            ISet<string> loadFieldNames = Support.Compatibility.SetFactory.CreateHashSet<string>();
            loadFieldNames.Add(DocHelper.TEXT_FIELD_1_KEY);
            loadFieldNames.Add(DocHelper.TEXT_FIELD_UTF1_KEY);
            ISet<string> lazyFieldNames = Support.Compatibility.SetFactory.CreateHashSet<string>();
            lazyFieldNames.Add(DocHelper.LARGE_LAZY_FIELD_KEY);
            lazyFieldNames.Add(DocHelper.LAZY_FIELD_KEY);
            lazyFieldNames.Add(DocHelper.LAZY_FIELD_BINARY_KEY);
            lazyFieldNames.Add(DocHelper.TEXT_FIELD_UTF2_KEY);
			SetBasedFieldSelector fieldSelector = new SetBasedFieldSelector(loadFieldNames, lazyFieldNames);
			Document doc = reader.Doc(0, fieldSelector);
			Assert.IsTrue(doc != null, "doc is null and it shouldn't be");
			IFieldable field = doc.GetFieldable(DocHelper.LAZY_FIELD_KEY);
			Assert.IsTrue(field != null, "field is null and it shouldn't be");
			Assert.IsTrue(field.IsLazy, "field is not lazy and it should be");
            reader.Dispose();

		    Assert.Throws<AlreadyClosedException>(() => { var value = field.StringValue; },
		                                          "did not hit AlreadyClosedException as expected");
		}
		
		[Test]
		public virtual void  TestLoadFirst()
		{
			Assert.IsTrue(dir != null);
			Assert.IsTrue(fieldInfos != null);
			FieldsReader reader = new FieldsReader(dir, TEST_SEGMENT_NAME, fieldInfos);
			Assert.IsTrue(reader != null);
			Assert.IsTrue(reader.Size() == 1);
			LoadFirstFieldSelector fieldSelector = new LoadFirstFieldSelector();
			Document doc = reader.Doc(0, fieldSelector);
			Assert.IsTrue(doc != null, "doc is null and it shouldn't be");
			int count = 0;
			var l = doc.GetFields();
			for (System.Collections.IEnumerator iter = l.GetEnumerator(); iter.MoveNext(); )
			{
				Field field = (Field) iter.Current;
				Assert.IsTrue(field != null, "field is null and it shouldn't be");
				System.String sv = field.StringValue;
				Assert.IsTrue(sv != null, "sv is null and it shouldn't be");
				count++;
			}
			Assert.IsTrue(count == 1, count + " does not equal: " + 1);
		}
		
		/// <summary> Not really a test per se, but we should have some way of assessing whether this is worthwhile.
		/// <p/>
		/// Must test using a File based directory
		/// 
		/// </summary>
		/// <throws>  Exception </throws>
		[Test]
		public virtual void  TestLazyPerformance()
		{
			System.String tmpIODir = AppSettings.Get("tempDir", "");
			System.String userName = System.Environment.UserName;
			System.String path = tmpIODir + System.IO.Path.DirectorySeparatorChar.ToString() + "lazyDir" + userName;
            System.IO.DirectoryInfo file = new System.IO.DirectoryInfo(path);
			_TestUtil.RmDir(file);
			FSDirectory tmpDir = FSDirectory.Open(file);
			Assert.IsTrue(tmpDir != null);
			
			IndexWriter writer = new IndexWriter(tmpDir, new WhitespaceAnalyzer(), true, IndexWriter.MaxFieldLength.LIMITED);
			writer.UseCompoundFile = false;
			writer.AddDocument(testDoc);
			writer.Close();
			
			Assert.IsTrue(fieldInfos != null);
			FieldsReader reader;
			long lazyTime = 0;
			long regularTime = 0;
			int length = 50;
            ISet<string> lazyFieldNames = Support.Compatibility.SetFactory.CreateHashSet<string>();
			lazyFieldNames.Add(DocHelper.LARGE_LAZY_FIELD_KEY);
			SetBasedFieldSelector fieldSelector = new SetBasedFieldSelector(Support.Compatibility.SetFactory.CreateHashSet<string>(), lazyFieldNames);
			
			for (int i = 0; i < length; i++)
			{
				reader = new FieldsReader(tmpDir, TEST_SEGMENT_NAME, fieldInfos);
				Assert.IsTrue(reader != null);
				Assert.IsTrue(reader.Size() == 1);
				
				Document doc;
				doc = reader.Doc(0, null); //Load all of them
				Assert.IsTrue(doc != null, "doc is null and it shouldn't be");
				IFieldable field = doc.GetFieldable(DocHelper.LARGE_LAZY_FIELD_KEY);
				Assert.IsTrue(field.IsLazy == false, "field is lazy");
				System.String value_Renamed;
				long start;
				long finish;
				start = (DateTime.Now.Ticks / TimeSpan.TicksPerMillisecond);
				//On my machine this was always 0ms.
				value_Renamed = field.StringValue;
				finish = (DateTime.Now.Ticks / TimeSpan.TicksPerMillisecond);
				Assert.IsTrue(value_Renamed != null, "value is null and it shouldn't be");
				Assert.IsTrue(field != null, "field is null and it shouldn't be");
				regularTime += (finish - start);
                reader.Dispose();
				reader = null;
				doc = null;
				//Hmmm, are we still in cache???
				System.GC.Collect();
				reader = new FieldsReader(tmpDir, TEST_SEGMENT_NAME, fieldInfos);
				doc = reader.Doc(0, fieldSelector);
				field = doc.GetFieldable(DocHelper.LARGE_LAZY_FIELD_KEY);
				Assert.IsTrue(field.IsLazy == true, "field is not lazy");
				start = (DateTime.Now.Ticks / TimeSpan.TicksPerMillisecond);
				//On my machine this took around 50 - 70ms
				value_Renamed = field.StringValue;
				finish = (DateTime.Now.Ticks / TimeSpan.TicksPerMillisecond);
				Assert.IsTrue(value_Renamed != null, "value is null and it shouldn't be");
				lazyTime += (finish - start);
                reader.Dispose();
			}
			System.Console.Out.WriteLine("Average Non-lazy time (should be very close to zero): " + regularTime / length + " ms for " + length + " reads");
			System.Console.Out.WriteLine("Average Lazy Time (should be greater than zero): " + lazyTime / length + " ms for " + length + " reads");
		}
		
		[Test]
		public virtual void  TestLoadSize()
		{
			FieldsReader reader = new FieldsReader(dir, TEST_SEGMENT_NAME, fieldInfos);
			Document doc;
			
			doc = reader.Doc(0, new AnonymousClassFieldSelector(this));
			IFieldable f1 = doc.GetFieldable(DocHelper.TEXT_FIELD_1_KEY);
			IFieldable f3 = doc.GetFieldable(DocHelper.TEXT_FIELD_3_KEY);
			IFieldable fb = doc.GetFieldable(DocHelper.LAZY_FIELD_BINARY_KEY);
			Assert.IsTrue(f1.IsBinary);
			Assert.IsTrue(!f3.IsBinary);
			Assert.IsTrue(fb.IsBinary);
			AssertSizeEquals(2 * DocHelper.FIELD_1_TEXT.Length, f1.GetBinaryValue());
			Assert.AreEqual(DocHelper.FIELD_3_TEXT, f3.StringValue);
            AssertSizeEquals(DocHelper.LAZY_FIELD_BINARY_BYTES.Length, fb.GetBinaryValue());

            reader.Dispose();
		}
		
		private void  AssertSizeEquals(int size, byte[] sizebytes)
		{
			Assert.AreEqual((byte) (Number.URShift(size, 24)), sizebytes[0]);
			Assert.AreEqual((byte) (Number.URShift(size, 16)), sizebytes[1]);
			Assert.AreEqual((byte) (Number.URShift(size, 8)), sizebytes[2]);
			Assert.AreEqual((byte) size, sizebytes[3]);
		}
		
		public class FaultyFSDirectory:Directory
		{
		    private bool isDisposed;
			internal FSDirectory fsDir;
			public FaultyFSDirectory(System.IO.DirectoryInfo dir)
			{
				fsDir = FSDirectory.Open(dir);
				interalLockFactory = fsDir.LockFactory;
			}
			public override IndexInput OpenInput(System.String name)
			{
				return new FaultyIndexInput(fsDir.OpenInput(name));
			}
			public override System.String[] ListAll()
			{
				return fsDir.ListAll();
			}
			public override bool FileExists(System.String name)
			{
				return fsDir.FileExists(name);
			}
			public override long FileModified(System.String name)
			{
				return fsDir.FileModified(name);
			}
			public override void  TouchFile(System.String name)
			{
				fsDir.TouchFile(name);
			}
			public override void  DeleteFile(System.String name)
			{
				fsDir.DeleteFile(name);
			}
			public override long FileLength(System.String name)
			{
				return fsDir.FileLength(name);
			}
			public override IndexOutput CreateOutput(System.String name)
			{
				return fsDir.CreateOutput(name);
			}

            protected override void Dispose(bool disposing)
            {
                if (isDisposed) return;

                if (disposing)
                {
                    if (fsDir != null)
                    {
                        fsDir.Close();
                    }
                }

                fsDir = null;
                isDisposed = true;
            }
		}
		
		private class FaultyIndexInput:BufferedIndexInput, System.ICloneable
		{
			internal IndexInput delegate_Renamed;
			internal static bool doFail;
		    private bool isDisposed;
			internal int count;
			internal FaultyIndexInput(IndexInput delegate_Renamed)
			{
				this.delegate_Renamed = delegate_Renamed;
			}
			private void  SimOutage()
			{
				if (doFail && count++ % 2 == 1)
				{
					throw new System.IO.IOException("Simulated network outage");
				}
			}
			public override void  ReadInternal(byte[] b, int offset, int length)
			{
				SimOutage();
				delegate_Renamed.ReadBytes(b, offset, length);
			}

			public override void  SeekInternal(long pos)
			{
				//simOutage();
				delegate_Renamed.Seek(pos);
			}

			public override long Length()
			{
				return delegate_Renamed.Length();
			}

            protected override void Dispose(bool disposing)
            {
                if (isDisposed) return;
                if (disposing)
                {
                    if (delegate_Renamed != null)
                    {
                        delegate_Renamed.Close();
                    }
                }

                delegate_Renamed = null;
                isDisposed = true;
            }

			public override System.Object Clone()
			{
				return new FaultyIndexInput((IndexInput) delegate_Renamed.Clone());
			}
		}
		
		// LUCENE-1262
		[Test]
		public virtual void  TestExceptions()
		{
			System.String tempDir = System.IO.Path.GetTempPath();
			if (tempDir == null)
				throw new System.IO.IOException("java.io.tmpdir undefined, cannot run test");
            System.IO.DirectoryInfo indexDir = new System.IO.DirectoryInfo(System.IO.Path.Combine(tempDir, "testfieldswriterexceptions"));
			
			try
			{
				Directory dir = new FaultyFSDirectory(indexDir);
				IndexWriter writer = new IndexWriter(dir, new WhitespaceAnalyzer(), true, IndexWriter.MaxFieldLength.LIMITED);
				for (int i = 0; i < 2; i++)
					writer.AddDocument(testDoc);
				writer.Optimize();
				writer.Close();
				
				IndexReader reader = IndexReader.Open(dir, true);
				
				FaultyIndexInput.doFail = true;
				
				bool exc = false;
				
				for (int i = 0; i < 2; i++)
				{
					try
					{
						reader.Document(i);
					}
					catch (System.IO.IOException ioe)
					{
						// expected
						exc = true;
					}
					try
					{
						reader.Document(i);
					}
					catch (System.IO.IOException ioe)
					{
						// expected
						exc = true;
					}
				}
				Assert.IsTrue(exc);
				reader.Close();
				dir.Close();
			}
			finally
			{
				_TestUtil.RmDir(indexDir);
			}
		}
	}
}