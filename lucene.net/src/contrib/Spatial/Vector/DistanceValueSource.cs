﻿/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

using System.Diagnostics;
using Lucene.Net.Index;
using Lucene.Net.Search;
using Lucene.Net.Search.Function;
using Lucene.Net.Spatial.Util;
using Spatial4n.Core.Distance;
using Spatial4n.Core.Shapes;
using Spatial4n.Core.Shapes.Impl;

namespace Lucene.Net.Spatial.Vector
{
	/// <summary>
    /// An implementation of the Lucene ValueSource model that returns the distance.
	/// </summary>
	public class DistanceValueSource : ValueSource
	{
		private readonly PointVectorStrategy strategy;
		private readonly Point from;

		public DistanceValueSource(PointVectorStrategy strategy, Point from)
		{
			this.strategy = strategy;
			this.from = from;
		}

		public class DistanceDocValues : DocValues
		{
			private readonly DistanceValueSource enclosingInstance;

			private readonly double[] ptX, ptY;
			private readonly IBits validX, validY;

            private readonly Point from;
            private readonly DistanceCalculator calculator;
            private readonly double nullValue;

			public DistanceDocValues(DistanceValueSource enclosingInstance, IndexReader reader)
			{
				this.enclosingInstance = enclosingInstance;

				ptX = FieldCache_Fields.DEFAULT.GetDoubles(reader, enclosingInstance.strategy.GetFieldNameX()/*, true*/);
				ptY = FieldCache_Fields.DEFAULT.GetDoubles(reader, enclosingInstance.strategy.GetFieldNameY()/*, true*/);
				validX = FieldCache_Fields.DEFAULT.GetDocsWithField(reader, enclosingInstance.strategy.GetFieldNameX());
				validY = FieldCache_Fields.DEFAULT.GetDocsWithField(reader, enclosingInstance.strategy.GetFieldNameY());

                from = enclosingInstance.from;
                calculator = enclosingInstance.strategy.GetSpatialContext().GetDistCalc();
                nullValue = (enclosingInstance.strategy.GetSpatialContext().IsGeo() ? 180 : double.MaxValue);
			}

			public override float FloatVal(int doc)
			{
				return (float)DoubleVal(doc);
			}

			public override double DoubleVal(int doc)
			{
				// make sure it has minX and area
				if (validX.Get(doc))
				{
				    Debug.Assert(validY.Get(doc));
					return calculator.Distance(from, ptX[doc], ptY[doc]);
				}
				return nullValue;
			}

			public override string ToString(int doc)
			{
				return enclosingInstance.Description() + "=" + FloatVal(doc);
			}
		}

		public override DocValues GetValues(IndexReader reader)
		{
			return new DistanceDocValues(this, reader);
		}

		public override string Description()
		{
            return "DistanceValueSource(" + strategy + ", " + from + ")";
		}

		public override bool Equals(object o)
		{
			if (this == o) return true;

			var that = o as DistanceValueSource;
			if (that == null) return false;

            if (!from.Equals(that.from)) return false;
            if (!strategy.Equals(that.strategy)) return false;

			return true;
		}

		public override int GetHashCode()
		{
		    return from.GetHashCode();
		}
	}
}
