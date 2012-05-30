package com.mucommander.aether.adapter;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.commons.io.FilenameUtils;
import org.jets3t.service.S3ObjectsChunk;
import org.jets3t.service.S3ServiceException;
import org.jets3t.service.model.S3Bucket;
import org.jets3t.service.model.S3Object;
import org.jets3t.service.model.S3Owner;
import org.jets3t.service.utils.ServiceUtils;

import com.tesis.aether.core.exception.ConnectionException;
import com.tesis.aether.core.exception.DeleteException;
import com.tesis.aether.core.exception.FileNotExistsException;
import com.tesis.aether.core.exception.MethodNotSupportedException;
import com.tesis.aether.core.exception.UploadException;
import com.tesis.aether.core.factory.ServiceFactory;
import com.tesis.aether.core.services.storage.ExtendedStorageService;
import com.tesis.aether.core.services.storage.object.StorageObjectMetadata;


public class AetherAdapter {
	private ExtendedStorageService	service = ServiceFactory.instance.getFirstStorageService();

	public AetherAdapter() throws S3ServiceException {
		try {
			service.connect(null);
		} catch (ConnectionException e) {
			throw new S3ServiceException(e);
		} catch (MethodNotSupportedException e) {
			throw new S3ServiceException(e);
		}
	}

	public S3Object getObject(String bucketName, String objectKey) throws S3ServiceException {
		try {

			if ((objectKey.endsWith("/") && service.checkDirectoryExists(bucketName, objectKey)) || (!objectKey.endsWith("/") && service.checkFileExists(bucketName, objectKey))) {
				StorageObjectMetadata storageObject = service.getMetadataForObject(bucketName, objectKey);
				S3Object object = new S3Object(objectKey);

				try {
					object.setDataInputStream(service.getInputStream(bucketName, objectKey));
				} catch (Exception e) {
					
				}

				object.addAllMetadata(generateJetS3tMetadata(storageObject));
				object.setBucketName(bucketName);
				object.setStorageClass("STANDARD");
				object.setETag(storageObject.getMd5hash());
				return object;
			}
			throw new S3ServiceException();
		} catch (Exception e) {
			throw new S3ServiceException(e);
		}
	}
	
	public S3Object getObjectDetails(String bucketName, String objectKey, Object o1, Object o2, Object o3, Object o4) throws S3ServiceException {
		return this.getObjectDetails(bucketName, objectKey);
	}

	public S3Object getObjectDetails(String bucketName, String objectKey) throws S3ServiceException {
		StorageObjectMetadata storageObject = service.getMetadataForObject(bucketName, objectKey);
		if (storageObject.getLength() == null && storageObject.getMd5hash() == null) {
			throw new S3ServiceException("ResponseStatus: Not Found.");
		}
		S3Object object = new S3Object(objectKey);
		object.addAllMetadata(generateJetS3tMetadata(storageObject));
		object.setStorageClass("STANDARD");
		object.setETag(storageObject.getMd5hash());
		return object;
	}

	public S3Object putObject(String bucketName, S3Object object) throws S3ServiceException {
		try {

			String name = FilenameUtils.getName(object.getKey());//.getName());
			String path = FilenameUtils.getPathNoEndSeparator(object.getKey());//.getName());

			if (object.getDataInputStream() != null) {
				service.uploadInputStream(object.getDataInputStream(), bucketName, path, name, object.getContentLength());
			}
			object.setLastModifiedDate(new Date());

			return object;
		} catch (UploadException e) {
			e.printStackTrace();
			return null;
		} catch (MethodNotSupportedException e) {
			e.printStackTrace();
			return null;
		} catch (FileNotExistsException e) {
			e.printStackTrace();
			return null;
		}
	}

	public void deleteBucket(String bucketName) throws S3ServiceException {
		try {
			service.deleteContainer(bucketName);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void deleteObject(String bucket, String objectKey) throws S3ServiceException {
		try {
			service.delete(bucket, objectKey, true);
		} catch (DeleteException e) {
			e.printStackTrace();
		}
	}

	private String[] getCommonPrefixes(List<StorageObjectMetadata> files) {
		List<String> retElements = new ArrayList<String>();
		for (StorageObjectMetadata metadata : files) {
			if (!metadata.isFile()) {
				retElements.add(metadata.getPathAndName() + "/");
			}
		}
		return (String[]) retElements.toArray(new String[retElements.size()]);
	}
	
	public S3ObjectsChunk listObjectsChunked(String bucketName, String prefix, String delimiter, long maxListingLength, String priorLastKey, boolean completeListing) throws S3ServiceException {
		return this.listObjectsInternal(bucketName, prefix, delimiter, maxListingLength, true, priorLastKey, null);
	}

	public S3ObjectsChunk listObjectsInternal(String bucketName,
			String prefix, String delimiter, long maxListingLength,
			boolean automaticallyMergeChunks, String priorLastKey,
			String priorLastVersion) throws S3ServiceException {
		List<StorageObjectMetadata> listFiles;
		S3ObjectsChunk storageObjectsChunk;
		// se obtienen todos los elementos (archivos y directorios)
		try {
			if (prefix != null) {
				listFiles = service.listFiles(bucketName, prefix, false);
			} else {
				listFiles = service.listFiles(bucketName, "", false);
			}
			// Se obtienen los S3Objects (archivos)
			S3Object[] objects = aetherMetadataListToS3ObjectArray(listFiles,
					bucketName);

			//Se obtienen los common prefixed (directorios)
			String[] commonPrefixes = getCommonPrefixes(listFiles);

			storageObjectsChunk = new S3ObjectsChunk(
					prefix, delimiter, objects, commonPrefixes, null);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		return storageObjectsChunk;
	}

	public S3Bucket createBucket(String bucketName) throws S3ServiceException {
		try {
			service.createContainer(bucketName);
			S3Bucket bucket = new S3Bucket();
			bucket.setName(bucketName);
			bucket.setOwner(new S3Owner());
			return bucket;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public S3Bucket[] listAllBuckets() throws S3ServiceException {
		try {
			List<StorageObjectMetadata> listContainers = service.listContainers();
			return toS3Bucket(listContainers);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * UTIL
	 */
	private S3Bucket[] toS3Bucket(List<StorageObjectMetadata> listContainers) {
		S3Bucket[] buckets = new S3Bucket[listContainers.size()];
		for (int i = 0; i < listContainers.size(); i++) {
			S3Bucket bucket = new S3Bucket(listContainers.get(i).getName());
			bucket.setOwner(new S3Owner());
			buckets[i] = bucket;
		}
		return buckets;
	}

	private Map<String, Object> generateJetS3tMetadata(StorageObjectMetadata metadata) {
		Map<String, Object> jets3metadata = new HashMap<String, Object>();
		jets3metadata.put(S3Object.METADATA_HEADER_LAST_MODIFIED_DATE, metadata.getLastModified());
		jets3metadata.put(S3Object.METADATA_HEADER_CONTENT_LENGTH, (metadata.getLength()!=null?metadata.getLength().toString():"0"));
		try {
			jets3metadata.put(S3Object.METADATA_HEADER_CONTENT_MD5, ServiceUtils.toBase64(ServiceUtils.fromHex(metadata.getMd5hash())));
		} catch (Exception e) {
		}
		return jets3metadata;
	}

	private S3Object[] aetherMetadataListToS3ObjectArray(List<StorageObjectMetadata> listFiles, String bucketName) {
		List<S3Object> jCloudsMetadata = new ArrayList<S3Object>();
		for (StorageObjectMetadata metadata : listFiles) {
			if (metadata.isFile()) {
				S3Object object = new S3Object(metadata.getPathAndName());
				object.setBucketName(bucketName);
				object.setStorageClass("STANDARD");
				object.setETag(metadata.getMd5hash());
				object.addAllMetadata(generateJetS3tMetadata(metadata));
				jCloudsMetadata.add(object);
			}
		}

		return (S3Object[]) jCloudsMetadata.toArray(new S3Object[jCloudsMetadata.size()]);
	}

	private String getDirectory(String object) {
		String directory = "";
		try {
			if (object.endsWith("/"))
				return object;
			String aux = (object.startsWith("/") ? object.substring(1) : object);
			String[] st = aux.split("/");
			if (st.length > 1) {
				for (int i = 0; i < st.length - 1; i++) {
					directory += st[i] + "/";
				}
			}
		} catch (Exception e) {
			Logger.getAnonymousLogger().warning(
					"Error al obtener el directorio de " + object
							+ "   -  Error: " + e.getMessage());
		}
		return directory;
	}

	private String getNameObject(String object) {
	String aux = object.replace("/", "/*/");
		String[] st = aux.split("/");
		if (st.length > 0 && !st[st.length - 1].equals("*")) {
			if (st.length > 1) {
				return st[st.length -1];
			} else {
				return st[st.length - 1];
			}
		} else {
			return "";
		}
	}
	
	@SuppressWarnings("unchecked")
	public Map<String, Object> copyObject(String bucketName, String objectKey,
			String bucketName2, S3Object destObject, boolean b) throws S3ServiceException {
        
		String nameDestObject = destObject.getKey();
		
		if (bucketName.equals(bucketName2)) {
			String dir1 = getDirectory(objectKey);
			String name1 = getNameObject(objectKey);
			String dir2 = getDirectory(nameDestObject);
			String name2 = getNameObject(nameDestObject);
			if (dir1.equals(dir2)) {
				if (name1.equals(name2))
					throw new S3ServiceException("No se puede copiar un elemento sobre si mismo.");
				if (!"".equals(name1) && !"".equals(name2)) {
					try {
						//Se debe descargar, renombrar y volver  asubir
						String fileName = System.currentTimeMillis() + name2;
						File f = File.createTempFile(fileName, "");
						f = new File((FilenameUtils.getFullPathNoEndSeparator(f.getCanonicalPath())));
						if (service.checkFileExists(bucketName, objectKey)) {
							service.downloadFileToDirectory(bucketName, objectKey, f);
							File to = new File(f, name2);
							to.deleteOnExit();
							f.renameTo(to);
							service.upload(to, bucketName2, dir2);
							return (Map<String, Object>) getObjectDetails(bucketName2, destObject.getKey()).getMetadataMap();
						} else {
							if (service.checkDirectoryExists(bucketName, objectKey)) {
								//por el momento queda sin hacer
							} else
								throw new S3ServiceException("Error desconocido al copiar los objetos.");
						}
					} catch (Exception e) {
						throw new S3ServiceException(e);
					}
				} else {
					throw new S3ServiceException("Error en los nombres de los archivos.");
				}
			}
		}
		try {
			service.copyFile(bucketName, objectKey, bucketName2, 
					destObject.getKey());
			return (Map<String, Object>) getObjectDetails(bucketName2, destObject.getKey()).getMetadataMap();
		} catch (Exception e) {
			throw new S3ServiceException(e);
		}
	}

	public S3Bucket getBucket(String bucketName) throws S3ServiceException {
		try {
			List<StorageObjectMetadata> listContainers = service.listContainers();
			for (StorageObjectMetadata som : listContainers) {
				if (som.getName().equals(bucketName)) {
					S3Bucket bucket = new S3Bucket(bucketName);
					bucket.setOwner(new S3Owner());
					bucket.setCreationDate(new Date());
					return bucket;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public S3Object getObject(String bucketName, String objectKey,
			Object object, Object object2, Object object3, Object object4,
			Object offset, Object object5) throws S3ServiceException {
		return this.getObject(bucketName, objectKey);
	}
}