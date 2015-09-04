module ActiveRecord
  module CollectionCacheKey

    def collection_cache_key(collection = all, timestamp_column = :updated_at) # :nodoc:
      query_signature = Digest::MD5.hexdigest(collection.to_sql)
      key = "#{collection.model_name.cache_key}/query-#{query_signature}"

      if collection.loaded?
        size = collection.size
        timestamp = collection.max_by(&timestamp_column).public_send(timestamp_column)
      else
        column_type = type_for_attribute(timestamp_column.to_s)
        column = "#{connection.quote_table_name(collection.table_name)}.#{connection.quote_column_name(timestamp_column)}"

        query = collection
          .select("COUNT(*) AS size", "MAX(#{column}) AS timestamp")
          .unscope(:order)
        result = connection.select_one(query)

        size = result["size"]
        timestamp = column_type.deserialize(result["timestamp"])
      end

      if timestamp
        "#{key}-#{size}-#{timestamp.utc.to_s(cache_timestamp_format)}"
      else
        "#{key}-#{size}"
      end
    end
  end
end
